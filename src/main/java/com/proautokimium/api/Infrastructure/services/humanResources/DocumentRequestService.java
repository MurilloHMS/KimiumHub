package com.proautokimium.api.Infrastructure.services.humanResources;

import com.proautokimium.api.Application.DTOs.humanResources.DocumentRequest.DocumentRequestDTO;
import com.proautokimium.api.Application.DTOs.humanResources.DocumentRequest.RecipientDTO;
import com.proautokimium.api.Application.DTOs.humanResources.DocumentRequest.RequestFileDTO;
import com.proautokimium.api.Infrastructure.exceptions.humanResources.DocumentRequestFileNotFoundException;
import com.proautokimium.api.Infrastructure.exceptions.humanResources.DocumentRequestNotFoundException;
import com.proautokimium.api.Infrastructure.exceptions.humanResources.DocumentRequestRecipientNotFoundException;
import com.proautokimium.api.Infrastructure.repositories.EmployeeRepository;
import com.proautokimium.api.Infrastructure.repositories.UserRepository;
import com.proautokimium.api.Infrastructure.repositories.humanResources.DocumentRequestFileRepository;
import com.proautokimium.api.Infrastructure.repositories.humanResources.DocumentRequestRecipientRepository;
import com.proautokimium.api.Infrastructure.repositories.humanResources.DocumentRequestRepository;
import com.proautokimium.api.Infrastructure.repositories.humanResources.EmployeeDocumentRepository;
import com.proautokimium.api.Infrastructure.repositories.humanResources.EmployeeDocumentTypeRepository;
import com.proautokimium.api.Infrastructure.services.notification.NotificationService;
import com.proautokimium.api.Infrastructure.services.storage.EmployeeDocumentStorageService;
import com.proautokimium.api.domain.enums.NotificationType;
import com.proautokimium.api.domain.enums.humanResources.RecipientStatus;
import com.proautokimium.api.domain.enums.humanResources.RequestStatus;
import com.proautokimium.api.domain.exceptions.humanResources.InvalidStatusTransitionException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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
import java.nio.file.Files;
import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class DocumentRequestService {

    private static final Logger log = LoggerFactory.getLogger(DocumentRequestService.class);

    /** Onde o funcionário vê e responde as solicitações dele. */
    private static final String EMPLOYEE_LINK = "/documentos/rh/requests";
    /** Onde o RH confere: a aba Solicitações da Pendências. */
    private static final String REVIEW_LINK = "/rh/pendencias";
    /** Quem confere é quem pode ALTERAR na tela do RH, a mesma regra do @PreAuthorize do aprovar. */
    private static final String REVIEW_SCREEN = "rh/document-requests";
    private static final String REVIEW_PERMISSION = "ALTERAR";

    private final DocumentRequestRepository documentRequestRepository;
    private final DocumentRequestRecipientRepository documentRequestRecipientRepository;
    private final Clock clock;
    private final EmployeeRepository employeeRepository;
    private final UserRepository userRepository;
    private final DocumentRequestFileRepository documentRequestFileRepository;
    private final EmployeeDocumentStorageService storage;
    private final EmployeeDocumentRepository employeeDocumentRepository;
    private final EmployeeDocumentTypeRepository employeeDocumentTypeRepository;
    private final NotificationService notificationService;

    public DocumentRequestService(DocumentRequestRepository documentRequestRepository, DocumentRequestRecipientRepository documentRequestRecipientRepository, Clock clock, EmployeeRepository employeeRepository, UserRepository userRepository, DocumentRequestFileRepository documentRequestFileRepository, EmployeeDocumentStorageService storage,
                                  EmployeeDocumentRepository employeeDocumentRepository, EmployeeDocumentTypeRepository employeeDocumentTypeRepository,
                                  NotificationService notificationService) {
        this.documentRequestRepository = documentRequestRepository;
        this.documentRequestRecipientRepository = documentRequestRecipientRepository;
        this.clock = clock;
        this.employeeRepository = employeeRepository;
        this.userRepository = userRepository;
        this.documentRequestFileRepository = documentRequestFileRepository;
        this.storage = storage;
        this.employeeDocumentRepository = employeeDocumentRepository;
        this.employeeDocumentTypeRepository = employeeDocumentTypeRepository;
        this.notificationService = notificationService;
    }

    @Transactional
    public DocumentRequest createDraft(String title, String login){
        DocumentRequest request = DocumentRequest.draft(title, login, LocalDateTime.now(clock));
        return documentRequestRepository.save(request);
    }

    @Transactional
    public DocumentRequest updateDraft(UUID id, String title, String instructions, LocalDate dueDate, List<RequestField> form){
        DocumentRequest request = documentRequestRepository.findById(id)
                .orElseThrow(DocumentRequestNotFoundException::new);
        request.updateDraft(title, instructions, dueDate, form);
        return documentRequestRepository.save(request);
    }

    @Transactional
    public DocumentRequest close(UUID id){
        DocumentRequest request = documentRequestRepository.findById(id)
                .orElseThrow(DocumentRequestNotFoundException::new);
        request.close(LocalDateTime.now(clock));
        return documentRequestRepository.save(request);
    }

    @Transactional
    public DocumentRequest send(UUID id, boolean all, Set<UUID> companyIds,
                                Set<UUID> departmentIds, Set<UUID> employeeIds){

        LocalDateTime now = LocalDateTime.now(clock);

        DocumentRequest request = documentRequestRepository.findById(id)
                .orElseThrow(DocumentRequestNotFoundException::new);

        // Antes do send: com público vazio, a solicitação continua rascunho e o RH corrige.
        List<Employee> audience = resolveAudience(all, companyIds, departmentIds, employeeIds);
        if(audience.isEmpty())
            throw new InvalidRequestDataException("Ninguém do público escolhido tem acesso ao sistema.");

        request.send(now);

        for(Employee employee : audience) {
            DocumentRequestRecipient recipient = DocumentRequestRecipient.create(request, employee, now);
            documentRequestRecipientRepository.save(recipient);
        }

        // Uma consulta para todos os logins, não uma por pessoa.
        List<UUID> ids = audience.stream().map(Employee::getId).toList();
        for (User user : userRepository.findActiveByEmployeeIds(ids)) {
            safely(() -> notificationService.notify(user.getLogin(), NotificationType.SOLICITACAO,
                    "Nova solicitação do RH", request.getTitle(), EMPLOYEE_LINK));
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

        notifyOwner(recipient, "Sua resposta foi aprovada", recipient.getDocumentRequest().getTitle());
        return documentRequestRecipientRepository.save(recipient);
    }

    @Transactional
    public DocumentRequestRecipient giveBack(UUID recipientId, String reviewerLogin, String reason){
        DocumentRequestRecipient recipient = documentRequestRecipientRepository.findById(recipientId)
                .orElseThrow(DocumentRequestRecipientNotFoundException::new);

        recipient.giveBack(reviewerLogin, reason, LocalDateTime.now(clock));
        notifyOwner(recipient, "Sua resposta foi devolvida",
                recipient.getDocumentRequest().getTitle() + ": " + recipient.getReturnReason());
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
        requireOpen(recipient);

        Map<String, Object> accepted = acceptedAnswers(recipient, answers == null ? Map.of() : answers);
        recipient.submit(accepted, LocalDateTime.now(clock));
        DocumentRequestRecipient saved = documentRequestRecipientRepository.save(recipient);

        String title = caller.getName() + " respondeu";
        for (String reviewer : userRepository.findActiveLoginsAllowed(REVIEW_SCREEN, REVIEW_PERMISSION)) {
            if (reviewer.equals(login)) continue;
            safely(() -> notificationService.notify(reviewer, NotificationType.SOLICITACAO,
                    title, recipient.getDocumentRequest().getTitle(), REVIEW_LINK));
        }
        return saved;
    }

    @Transactional
    public DocumentRequestFile upload(UUID recipientId, String login, String fieldKey, MultipartFile file) throws IOException {
        DocumentRequestRecipient recipient = documentRequestRecipientRepository.findById(recipientId)
                .orElseThrow(DocumentRequestRecipientNotFoundException::new);

        Employee caller = resolveEmployee(login);
        if(caller == null) throw new EmployeeNotFoundException();

        if(!isOwner(recipient, caller)) throw new DocumentRequestRecipientNotFoundException();
        requireOpen(recipient);
        if(recipient.getStatus() != RecipientStatus.PENDING && recipient.getStatus() != RecipientStatus.RETURNED)
            throw new InvalidStatusTransitionException("Esta resposta já foi enviada; aguarde a conferência do RH.");

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

    // ── Leituras ─────────────────────────────────────────────────────────────
    // Devolvem DTO montado aqui dentro, com a transação aberta: o funcionário e
    // a solicitação são LAZY, e fora dela não dá para ler o nome.

    @Transactional(readOnly = true)
    public List<DocumentRequestDTO> listRequests(){
        Map<UUID, Map<RecipientStatus, Long>> counts = new HashMap<>();
        for (DocumentRequestRecipientRepository.StatusCount c : documentRequestRecipientRepository.countByRequestAndStatus()) {
            counts.computeIfAbsent(c.getRequestId(), k -> new EnumMap<>(RecipientStatus.class)).put(c.getStatus(), c.getTotal());
        }
        return documentRequestRepository.findAllByOrderByCreatedAtDesc().stream()
                .map(r -> toDto(r, counts.getOrDefault(r.getId(), Map.of())))
                .toList();
    }

    @Transactional(readOnly = true)
    public DocumentRequestDTO getRequest(UUID id){
        DocumentRequest request = documentRequestRepository.findById(id)
                .orElseThrow(DocumentRequestNotFoundException::new);
        Map<RecipientStatus, Long> counts = documentRequestRecipientRepository.findByDocumentRequestOrderByAddedAtDesc(request)
                .stream().collect(Collectors.groupingBy(DocumentRequestRecipient::getStatus, () -> new EnumMap<>(RecipientStatus.class), Collectors.counting()));
        return toDto(request, counts);
    }

    /** As respostas de uma solicitação, para o RH acompanhar. */
    @Transactional(readOnly = true)
    public List<RecipientDTO> listRecipients(UUID requestId){
        DocumentRequest request = documentRequestRepository.findById(requestId)
                .orElseThrow(DocumentRequestNotFoundException::new);
        return toDtos(documentRequestRecipientRepository.findByDocumentRequestOrderByAddedAtDesc(request));
    }

    /** O que espera conferência, de todas as solicitações: a aba da Pendências. */
    @Transactional(readOnly = true)
    public List<RecipientDTO> listAwaitingReview(){
        List<DocumentRequestRecipient> submitted = new ArrayList<>(documentRequestRecipientRepository.findByStatus(RecipientStatus.SUBMITTED));
        // A mais antiga primeiro: é a que espera há mais tempo.
        submitted.sort(Comparator.comparing(DocumentRequestRecipient::getSubmittedAt));
        return toDtos(submitted);
    }

    /** As solicitações de quem está logado. O funcionário vem do login, nunca de um id que chegou de fora. */
    @Transactional(readOnly = true)
    public List<RecipientDTO> listMine(String login){
        Employee caller = resolveEmployee(login);
        if(caller == null) throw new EmployeeNotFoundException();
        return toDtos(documentRequestRecipientRepository.findByEmployeeOrderByAddedAtDesc(caller));
    }

    @Transactional(readOnly = true)
    public RecipientDTO getRecipient(UUID recipientId){
        DocumentRequestRecipient recipient = documentRequestRecipientRepository.findById(recipientId)
                .orElseThrow(DocumentRequestRecipientNotFoundException::new);
        return toDtos(List.of(recipient)).get(0);
    }

    public record FileContent(String filename, String contentType, byte[] bytes) {}

    /**
     * O arquivo de uma resposta: o RH baixa qualquer um; o funcionário, só os
     * dele. Para quem não é dono, 404, igual a "não existe".
     */
    @Transactional(readOnly = true)
    public FileContent readFile(UUID fileId, String login, boolean isReviewer) throws IOException {
        DocumentRequestFile file = documentRequestFileRepository.findById(fileId)
                .orElseThrow(DocumentRequestFileNotFoundException::new);
        if(!isReviewer){
            Employee caller = resolveEmployee(login);
            if(caller == null || !isOwner(file.getDocumentRequestRecipient(), caller))
                throw new DocumentRequestFileNotFoundException();
        }
        java.nio.file.Path path = storage.resolve(file.getStoragePath());
        // Registro sem arquivo no disco é caso real (ver a galeria): 404, e não 500.
        if(!Files.exists(path)) throw new DocumentRequestFileNotFoundException();
        return new FileContent(file.getOriginalFilename(),
                EmployeeDocumentService.contentTypeOf(file.getOriginalFilename()), Files.readAllBytes(path));
    }

    // Methods

    /** Só se responde a solicitação aberta: encerrada não aceita mais nada, nem arquivo. */
    private static void requireOpen(DocumentRequestRecipient recipient){
        if(recipient.getDocumentRequest().getStatus() != RequestStatus.OPEN)
            throw new InvalidStatusTransitionException("Esta solicitação foi encerrada.");
    }

    /**
     * As respostas que valem: só as dos campos do formulário que não são arquivo
     * (chave inventada é descartada), e todo campo obrigatório preenchido. O
     * campo de arquivo obrigatório conta como preenchido se tem arquivo atual.
     */
    private Map<String, Object> acceptedAnswers(DocumentRequestRecipient recipient, Map<String, Object> answers){
        Set<String> withFile = documentRequestFileRepository.findByDocumentRequestRecipientAndReplacedAtIsNull(recipient)
                .stream().map(DocumentRequestFile::getFieldKey).collect(Collectors.toSet());

        Map<String, Object> accepted = new HashMap<>();
        for (RequestField field : recipient.getDocumentRequest().getForm()) {
            if (field.isFile()) {
                if (field.required() && !withFile.contains(field.key()))
                    throw new InvalidRequestDataException("Envie o arquivo de \"" + field.label() + "\".");
                continue;
            }
            Object value = answers.get(field.key());
            boolean empty = value == null || (value instanceof String text && text.isBlank());
            if (field.required() && empty)
                throw new InvalidRequestDataException("Preencha \"" + field.label() + "\".");
            if (!empty) accepted.put(field.key(), value);
        }
        return accepted;
    }

    private static DocumentRequestDTO toDto(DocumentRequest r, Map<RecipientStatus, Long> byStatus){
        long pending = byStatus.getOrDefault(RecipientStatus.PENDING, 0L);
        long submitted = byStatus.getOrDefault(RecipientStatus.SUBMITTED, 0L);
        long approved = byStatus.getOrDefault(RecipientStatus.APPROVED, 0L);
        long returned = byStatus.getOrDefault(RecipientStatus.RETURNED, 0L);
        return new DocumentRequestDTO(r.getId(), r.getTitle(), r.getInstructions(), r.getDueDate(), r.getStatus(),
                r.getForm(), r.getCreatedBy(), r.getCreatedAt(), r.getSentAt(), r.getClosedAt(),
                new DocumentRequestDTO.Counts(pending + submitted + approved + returned, pending, submitted, approved, returned));
    }

    /** Os arquivos atuais de todas as respostas numa consulta só, e não uma por pessoa. */
    private List<RecipientDTO> toDtos(List<DocumentRequestRecipient> recipients){
        if (recipients.isEmpty()) return List.of();
        Map<UUID, List<RequestFileDTO>> files = documentRequestFileRepository
                .findByDocumentRequestRecipientInAndReplacedAtIsNull(recipients).stream()
                .collect(Collectors.groupingBy(f -> f.getDocumentRequestRecipient().getId(),
                        Collectors.mapping(f -> new RequestFileDTO(f.getId(), f.getFieldKey(), f.getOriginalFilename(), f.getUploadedAt()),
                                Collectors.toList())));
        return recipients.stream().map(r -> {
            DocumentRequest request = r.getDocumentRequest();
            return new RecipientDTO(r.getId(), request.getId(), request.getTitle(), request.getInstructions(),
                    request.getDueDate(), request.getStatus(), request.getForm(),
                    r.getEmployee().getId(), r.getEmployee().getName(), r.getStatus(), r.getAnswers(),
                    r.getAddedAt(), r.getSubmittedAt(), r.getReviewedBy(), r.getReviewedAt(), r.getReturnReason(),
                    files.getOrDefault(r.getId(), List.of()));
        }).toList();
    }

    private void notifyOwner(DocumentRequestRecipient recipient, String title, String message){
        userRepository.findByEmployee_Id(recipient.getEmployee().getId()).ifPresent(user ->
                safely(() -> notificationService.notify(user.getLogin(), NotificationType.SOLICITACAO,
                        title, message, EMPLOYEE_LINK)));
    }

    /** O aviso é melhor esforço: a solicitação foi gravada, e isso não se desfaz porque o sino falhou. */
    private void safely(Runnable send){
        try {
            send.run();
        } catch (RuntimeException e) {
            log.warn("Falha ao avisar sobre solicitação do RH", e);
        }
    }
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

    /**
     * Quem recebe: uma FOTOGRAFIA do público na hora do envio. Contratado depois
     * não recebe; o RH acrescenta a pessoa à mão.
     *
     * Elegível é quem os Eventos já convidam: ativo e com login ativo. Sem login,
     * a pessoa não teria como responder.
     *
     * O setor vem pela EQUIPE: `Employee.department` é o enum antigo.
     */
    private List<Employee> resolveAudience(boolean all, Set<UUID> companyIds, Set<UUID> departmentsIds, Set<UUID> employeeIds){
        List<Employee> eligible = employeeRepository.findInvitable();
        if(all)
            return eligible;

        if(companyIds.isEmpty() && departmentsIds.isEmpty() && employeeIds.isEmpty())
            throw new InvalidRequestDataException("Escolha pelo menos uma empresa, um setor ou uma pessoa.");

        return eligible.stream()
                .filter(e -> (e.getCompany() != null && companyIds.contains(e.getCompany().getId()))
                        || (e.getTeam() != null && e.getTeam().getDepartment() != null
                            && departmentsIds.contains(e.getTeam().getDepartment().getId()))
                        || employeeIds.contains(e.getId()))
                .toList();
    }
}
