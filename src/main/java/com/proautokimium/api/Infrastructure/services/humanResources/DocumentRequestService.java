package com.proautokimium.api.Infrastructure.services.humanResources;

import com.proautokimium.api.Infrastructure.exceptions.humanResources.DocumentRequestNotFoundException;
import com.proautokimium.api.Infrastructure.exceptions.humanResources.DocumentRequestRecipientNotFoundException;
import com.proautokimium.api.Infrastructure.repositories.EmployeeRepository;
import com.proautokimium.api.Infrastructure.repositories.UserRepository;
import com.proautokimium.api.Infrastructure.repositories.humanResources.DocumentRequestFileRepository;
import com.proautokimium.api.Infrastructure.repositories.humanResources.DocumentRequestRecipientRepository;
import com.proautokimium.api.Infrastructure.repositories.humanResources.DocumentRequestRepository;
import com.proautokimium.api.Infrastructure.repositories.humanResources.EmployeeDocumentRepository;
import com.proautokimium.api.Infrastructure.repositories.humanResources.EmployeeDocumentTypeRepository;
import com.proautokimium.api.Infrastructure.services.storage.EmployeeDocumentStorageService;
import com.proautokimium.api.domain.entities.Employee;
import com.proautokimium.api.domain.entities.auth.User;
import com.proautokimium.api.domain.entities.humanResources.DocumentRequest;
import com.proautokimium.api.domain.entities.humanResources.DocumentRequestFile;
import com.proautokimium.api.domain.entities.humanResources.DocumentRequestRecipient;
import com.proautokimium.api.domain.entities.humanResources.EmployeeDocument;
import com.proautokimium.api.domain.valueObjects.humanResources.RequestField;
import com.proautokimium.api.domain.exceptions.humanResources.InvalidRequestDataException;
import com.proautokimium.api.domain.exceptions.partners.EmployeeNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@Service
public class DocumentRequestService {

    private final DocumentRequestRepository documentRequestRepository;
    private final DocumentRequestRecipientRepository documentRequestRecipientRepository;
    private final Clock clock;
    private final EmployeeRepository employeeRepository;
    private final UserRepository userRepository;
    private final DocumentRequestFileRepository documentRequestFileRepository;
    private final EmployeeDocumentStorageService storage;
    private final EmployeeDocumentRepository employeeDocumentRepository;
    private final EmployeeDocumentTypeRepository employeeDocumentTypeRepository;

    public DocumentRequestService(DocumentRequestRepository documentRequestRepository, DocumentRequestRecipientRepository documentRequestRecipientRepository, Clock clock, EmployeeRepository employeeRepository, UserRepository userRepository, DocumentRequestFileRepository documentRequestFileRepository, EmployeeDocumentStorageService storage,
                                  EmployeeDocumentRepository employeeDocumentRepository, EmployeeDocumentTypeRepository employeeDocumentTypeRepository) {
        this.documentRequestRepository = documentRequestRepository;
        this.documentRequestRecipientRepository = documentRequestRecipientRepository;
        this.clock = clock;
        this.employeeRepository = employeeRepository;
        this.userRepository = userRepository;
        this.documentRequestFileRepository = documentRequestFileRepository;
        this.storage = storage;
        this.employeeDocumentRepository = employeeDocumentRepository;
        this.employeeDocumentTypeRepository = employeeDocumentTypeRepository;
    }

    @Transactional
    public DocumentRequest createDraft(String title, String login){
        DocumentRequest request = DocumentRequest.draft(title, login, LocalDateTime.now(clock));
        return documentRequestRepository.save(request);
    }

    @Transactional
    public DocumentRequest send(UUID id, List<UUID> employeeIds){

        LocalDateTime now = LocalDateTime.now(clock);

        DocumentRequest request = documentRequestRepository.findById(id)
                .orElseThrow(DocumentRequestNotFoundException::new);

        request.send(now);

        for(UUID employeeID : employeeIds) {
            Employee employee = employeeRepository.findById(employeeID)
                    .orElseThrow(EmployeeNotFoundException::new);

            DocumentRequestRecipient recipient = DocumentRequestRecipient.create(request, employee, now);
            documentRequestRecipientRepository.save(recipient);
        }
        return documentRequestRepository.save(request);
    }

    @Transactional
    public DocumentRequestRecipient approve(UUID recipientId, String reviewerLogin){
        DocumentRequestRecipient recipient = documentRequestRecipientRepository.findById(recipientId)
                .orElseThrow(DocumentRequestRecipientNotFoundException::new);

        // A mesma hora na aprovação e nos documentos que ela cria.
        LocalDateTime now = LocalDateTime.now(clock);
        recipient.approve(reviewerLogin, now);

        // Cada arquivo atual cujo campo tem tipo de documento vira um documento do
        // funcionário, apontando para o MESMO arquivo no disco: não há cópia.
        List<RequestField> form = recipient.getDocumentRequest().getForm();
        for (DocumentRequestFile file : documentRequestFileRepository.findByDocumentRequestRecipientAndReplacedAtIsNull(recipient)) {
            RequestField field = form.stream()
                    .filter(f -> file.getFieldKey().equals(f.key()))
                    .findFirst()
                    .orElse(null);
            if (field == null || field.documentTypeId() == null) continue;

            EmployeeDocument document = new EmployeeDocument();
            document.setEmployee(recipient.getEmployee());
            // Sem FK no formulário: tipo apagado depois do envio vira documento sem tipo, não recusa.
            document.setType(employeeDocumentTypeRepository.findById(field.documentTypeId()).orElse(null));
            document.setTitle(field.label());
            document.setOriginalFilename(file.getOriginalFilename());
            document.setStoragePath(file.getStoragePath());
            document.setContentType(EmployeeDocumentService.contentTypeOf(file.getOriginalFilename()));
            document.setUploadedAt(now);
            document.setUploadedBy(reviewerLogin);

            file.linkTo(employeeDocumentRepository.save(document));
            documentRequestFileRepository.save(file);
        }

        return documentRequestRecipientRepository.save(recipient);
    }

    @Transactional
    public DocumentRequestRecipient giveBack(UUID recipientId, String reviewerLogin, String reason){
        DocumentRequestRecipient recipient = documentRequestRecipientRepository.findById(recipientId)
                .orElseThrow(DocumentRequestRecipientNotFoundException::new);

        recipient.giveBack(reviewerLogin, reason, LocalDateTime.now(clock));
        return documentRequestRecipientRepository.save(recipient);
    }

    @Transactional
    public DocumentRequestRecipient submit(UUID recipientId, String login, Map<String, Object> answers){
        DocumentRequestRecipient recipient = documentRequestRecipientRepository.findById(recipientId)
                .orElseThrow(DocumentRequestRecipientNotFoundException::new);

        Employee caller = resolveEmployee(login);
        if(caller == null) throw new EmployeeNotFoundException();

        // Quem não é dono recebe o mesmo 404 de "não existe": assim não descobre quais IDs existem.
        if(!isOwner(recipient, caller)) throw new DocumentRequestRecipientNotFoundException();

        recipient.submit(answers, LocalDateTime.now(clock));
        return documentRequestRecipientRepository.save(recipient);
    }

    @Transactional
    public DocumentRequestFile upload(UUID recipientId, String login, String fieldKey, MultipartFile file) throws IOException {
        DocumentRequestRecipient recipient = documentRequestRecipientRepository.findById(recipientId)
                .orElseThrow(DocumentRequestRecipientNotFoundException::new);

        Employee caller = resolveEmployee(login);
        if(caller == null) throw new EmployeeNotFoundException();

        if(!isOwner(recipient, caller)) throw new DocumentRequestRecipientNotFoundException();

        boolean isFile = recipient.getDocumentRequest().getForm().stream()
                .anyMatch(f -> f.key().equals(fieldKey) && "FILE".equals(f.type()));

        if(!isFile) throw new InvalidRequestDataException("Este campo não pede arquivo");

        // todo: Separar e criar validador proprio
        EmployeeDocumentService.acceptedContentType(file);

        Optional<DocumentRequestFile> current = documentRequestFileRepository.findByDocumentRequestRecipientAndFieldKeyAndReplacedAtIsNull(recipient, fieldKey);
        LocalDateTime now = LocalDateTime.now(clock);
        current.ifPresent(old -> {
            old.replace(now);
            documentRequestFileRepository.saveAndFlush(old);
        });

        String storagePath = storage.save(file.getBytes(), caller.getCodParceiro(), file.getOriginalFilename());

        // O disco não participa da transação: recusado no banco, o arquivo é apagado à mão.
        try{
            DocumentRequestFile created = DocumentRequestFile.create(recipient,fieldKey, file.getOriginalFilename(), storagePath, now);
            return documentRequestFileRepository.save(created);
        } catch (RuntimeException refused) {
            try {
                storage.delete(storagePath);
            } catch (IOException deleteFailure) {
                // A pessoa precisa ver o motivo da recusa, não o erro do disco.
                refused.addSuppressed(deleteFailure);
            }
            throw refused;
        }
    }

    // Methods
    private static boolean isOwner(DocumentRequestRecipient recipient, Employee caller){
        Employee owner = recipient.getEmployee();
        return owner == caller || (caller.getId() != null && caller.getId().equals(owner.getId()));
    }

    private Employee resolveEmployee(String login){
        Employee viaLink = userRepository.findByLoginWithEmployee(login)
                .map(User::getEmployee)
                .orElse(null);
        if(viaLink != null) return viaLink;
        return employeeRepository.findByUsername(login).orElse(null);
    }
}
