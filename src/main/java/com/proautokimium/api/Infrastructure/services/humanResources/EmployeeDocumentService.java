package com.proautokimium.api.Infrastructure.services.humanResources;

import com.proautokimium.api.Application.DTOs.humanResources.EmployeeDocument.EmployeeDocumentResponseDTO;
import com.proautokimium.api.Application.DTOs.humanResources.EmployeeDocument.EmployeeDocumentUpdateDTO;
import com.proautokimium.api.Infrastructure.exceptions.humanResources.EmployeeDocumentNotFoundException;
import com.proautokimium.api.Infrastructure.exceptions.humanResources.EmployeeDocumentTypeNotFoundException;
import com.proautokimium.api.Infrastructure.repositories.EmployeeRepository;
import com.proautokimium.api.Infrastructure.repositories.UserRepository;
import com.proautokimium.api.Infrastructure.repositories.humanResources.EmployeeDocumentRepository;
import com.proautokimium.api.Infrastructure.repositories.humanResources.EmployeeDocumentTypeRepository;
import com.proautokimium.api.Infrastructure.services.notification.NotificationService;
import com.proautokimium.api.Infrastructure.services.storage.EmployeeDocumentStorageService;
import com.proautokimium.api.domain.entities.Employee;
import com.proautokimium.api.domain.entities.humanResources.EmployeeDocument;
import com.proautokimium.api.domain.entities.humanResources.EmployeeDocumentType;
import com.proautokimium.api.domain.enums.NotificationType;
import com.proautokimium.api.domain.enums.humanResources.EmployeeDocumentStatus;
import com.proautokimium.api.domain.exceptions.humanResources.InvalidRequestDataException;
import com.proautokimium.api.domain.exceptions.partners.EmployeeNotFoundException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;

@Service
public class EmployeeDocumentService {

    private static final Logger log = LoggerFactory.getLogger(EmployeeDocumentService.class);

    /** 10 MB: um PDF escaneado de contrato cabe; um vídeo por engano, não. */
    static final long MAX_FILE_BYTES = 10L * 1024 * 1024;

    /** O tipo guardado vem da EXTENSÃO conferida, não do que o navegador declarou. */
    private static final Map<String, String> ACCEPTED = Map.of(
            "pdf", "application/pdf",
            "jpg", "image/jpeg",
            "jpeg", "image/jpeg",
            "png", "image/png");

    private final EmployeeDocumentRepository repository;
    private final EmployeeDocumentTypeRepository typeRepository;
    private final EmployeeRepository employeeRepository;
    private final UserRepository userRepository;
    private final EmployeeDocumentStorageService storage;
    private final NotificationService notificationService;
    private final Clock clock;

    public EmployeeDocumentService(
            EmployeeDocumentRepository repository,
            EmployeeDocumentTypeRepository typeRepository,
            EmployeeRepository employeeRepository,
            UserRepository userRepository,
            EmployeeDocumentStorageService storage,
            NotificationService notificationService,
            Clock clock
    ) {
        this.repository = repository;
        this.typeRepository = typeRepository;
        this.employeeRepository = employeeRepository;
        this.userRepository = userRepository;
        this.storage = storage;
        this.notificationService = notificationService;
        this.clock = clock;
    }

    /**
     * O RH vincula um documento a um funcionário.
     *
     * **Tudo que pode recusar é conferido ANTES de gravar o arquivo** — arquivo,
     * funcionário, tipo, o documento substituído. Só a gravação no banco fica
     * depois, e se ela falhar o arquivo é apagado (o mesmo cuidado do reembolso,
     * #198): senão sobra arquivo no disco sem linha que aponte para ele.
     */
    @Transactional
    public EmployeeDocumentResponseDTO link(UUID employeeId, UUID typeId, String title, LocalDate dueDate,
                                            UUID replacesId, MultipartFile file, String uploadedBy) throws IOException {
        String contentType = acceptedContentType(file);

        Employee employee = employeeRepository.findById(employeeId)
                .orElseThrow(EmployeeNotFoundException::new);

        EmployeeDocumentType type = typeRepository.findById(typeId)
                .filter(EmployeeDocumentType::isActive)
                .orElseThrow(() -> new InvalidRequestDataException("Escolha um tipo de documento ativo."));

        EmployeeDocument replaced = replacesId == null ? null
                : repository.findById(replacesId).orElseThrow(EmployeeDocumentNotFoundException::new);

        String finalTitle = title == null || title.isBlank() ? type.getName() : title.trim();
        if (finalTitle.length() > 200) {
            throw new InvalidRequestDataException("O título passa de 200 caracteres.");
        }

        String storagePath = storage.save(file.getBytes(), employee.getCodParceiro(), file.getOriginalFilename());

        try {
            LocalDateTime now = LocalDateTime.now(clock);

            EmployeeDocument document = new EmployeeDocument();
            document.setEmployee(employee);
            document.setType(type);
            document.setTitle(finalTitle);
            document.setDueDate(dueDate);
            document.setOriginalFilename(file.getOriginalFilename());
            document.setStoragePath(storagePath);
            document.setContentType(contentType);
            document.setSizeBytes(file.getSize());
            document.setUploadedAt(now);
            document.setUploadedBy(uploadedBy);

            EmployeeDocument saved = repository.save(document);

            // Depois de salvo: o substituído aponta para o novo, então o novo
            // precisa de id. Outro funcionário é recusado pela entidade.
            if (replaced != null) {
                replaced.replaceWith(saved, now);
            }

            notifyEmployee(employee, finalTitle);
            return toResponse(saved, LocalDate.now(clock));
        } catch (RuntimeException refused) {
            try {
                storage.delete(storagePath);
            } catch (IOException deleteFailure) {
                refused.addSuppressed(deleteFailure);
            }
            throw refused;
        }
    }

    /** A lista do RH, com filtros opcionais. O status é filtrado aqui porque é calculado. */
    @Transactional(readOnly = true)
    public List<EmployeeDocumentResponseDTO> search(UUID employeeId, UUID typeId, EmployeeDocumentStatus status) {
        LocalDate today = LocalDate.now(clock);
        return repository.search(employeeId, typeId).stream()
                .filter(document -> status == null || document.statusOn(today) == status)
                .map(document -> toResponse(document, today))
                .toList();
    }

    /** Os documentos do funcionário vinculado ao login autenticado. */
    @Transactional(readOnly = true)
    public List<EmployeeDocumentResponseDTO> listMine(String login) {
        Employee employee = resolveEmployee(login);
        if (employee == null) return List.of();

        LocalDate today = LocalDate.now(clock);
        return repository.findMine(employee.getId()).stream()
                .map(document -> toResponse(document, today))
                .toList();
    }

    /** Corrige título, tipo e vencimento. O arquivo não muda: para isso, substitui. */
    @Transactional
    public EmployeeDocumentResponseDTO update(UUID id, EmployeeDocumentUpdateDTO dto) {
        EmployeeDocument document = repository.findById(id).orElseThrow(EmployeeDocumentNotFoundException::new);

        if (dto.typeId() != null) {
            EmployeeDocumentType type = typeRepository.findById(dto.typeId())
                    .orElseThrow(EmployeeDocumentTypeNotFoundException::new);
            document.setType(type);
        }
        document.setTitle(dto.title().trim());
        document.setDueDate(dto.dueDate());

        return toResponse(document, LocalDate.now(clock));
    }

    /**
     * Exclui o documento e, só depois do commit, o arquivo. Na ordem inversa, um
     * rollback deixaria a linha no banco apontando para um arquivo que sumiu.
     *
     * Se este era o SUBSTITUTO de outro, o banco solta o antigo
     * (`ON DELETE SET NULL`), que volta a valer — e a gerar aviso.
     */
    @Transactional
    public void delete(UUID id) {
        EmployeeDocument document = repository.findById(id).orElseThrow(EmployeeDocumentNotFoundException::new);
        String storagePath = document.getStoragePath();
        repository.delete(document);
        afterCommit(() -> {
            try {
                storage.delete(storagePath);
            } catch (IOException e) {
                // A linha já saiu; o arquivo que sobrou não aparece para ninguém.
                log.warn("Documento {} excluído, mas o arquivo {} ficou no disco", id, storagePath, e);
            }
        });
    }

    public Optional<EmployeeDocument> find(UUID id) {
        return repository.findById(id);
    }

    /** O dono do documento, ou quem tem a tela do RH. */
    public boolean canAccess(EmployeeDocument document, String login, boolean isHr) {
        if (isHr) return true;
        Employee employee = resolveEmployee(login);
        return employee != null && document.getEmployee().getId().equals(employee.getId());
    }

    public byte[] readFile(EmployeeDocument document) throws IOException {
        return Files.readAllBytes(storage.resolve(document.getStoragePath()));
    }

    /** Arquivo presente, até 10 MB, e PDF, JPG ou PNG pela extensão. */
    private String acceptedContentType(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new InvalidRequestDataException("Envie o arquivo do documento.");
        }
        if (file.getSize() > MAX_FILE_BYTES) {
            throw new InvalidRequestDataException("O arquivo passa de 10 MB.");
        }
        String name = file.getOriginalFilename() == null ? "" : file.getOriginalFilename();
        String extension = name.contains(".")
                ? name.substring(name.lastIndexOf('.') + 1).toLowerCase(Locale.ROOT)
                : "";
        String contentType = ACCEPTED.get(extension);
        if (contentType == null) {
            throw new InvalidRequestDataException("Envie um PDF, JPG ou PNG.");
        }
        return contentType;
    }

    private void notifyEmployee(Employee employee, String title) {
        userRepository.findByEmployee_Id(employee.getId()).ifPresent(user ->
                notificationService.notify(user.getLogin(), NotificationType.DOCUMENTO,
                        "Novo documento disponível",
                        "O RH vinculou um novo documento ao seu cadastro: " + title,
                        "/documentos/rh/documents"));
    }

    private void afterCommit(Runnable action) {
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    action.run();
                }
            });
        } else {
            action.run();
        }
    }

    private Employee resolveEmployee(String login) {
        Employee viaLink = userRepository.findByLoginWithEmployee(login)
                .map(user -> user.getEmployee())
                .orElse(null);
        if (viaLink != null) return viaLink;
        return employeeRepository.findByUsername(login).orElse(null);
    }

    private EmployeeDocumentResponseDTO toResponse(EmployeeDocument document, LocalDate today) {
        EmployeeDocumentType type = document.getType();
        EmployeeDocument replacedBy = document.getReplacedBy();
        return new EmployeeDocumentResponseDTO(
                document.getId(),
                document.getEmployee().getId(),
                document.getEmployee().getName(),
                type == null ? null : type.getId(),
                type == null ? null : type.getName(),
                document.getTitle(),
                document.getOriginalFilename(),
                document.getContentType(),
                document.getSizeBytes(),
                document.getDueDate(),
                document.statusOn(today),
                document.getDueDate() == null ? null : document.daysUntilDue(today),
                replacedBy == null ? null : replacedBy.getId(),
                document.getUploadedAt(),
                document.getUploadedBy()
        );
    }
}