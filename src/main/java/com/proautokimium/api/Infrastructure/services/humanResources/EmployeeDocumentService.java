package com.proautokimium.api.Infrastructure.services.humanResources;

import com.proautokimium.api.Application.DTOs.humanResources.EmployeeDocument.EmployeeDocumentResponseDTO;
import com.proautokimium.api.Infrastructure.repositories.EmployeeRepository;
import com.proautokimium.api.Infrastructure.repositories.UserRepository;
import com.proautokimium.api.Infrastructure.repositories.humanResources.EmployeeDocumentRepository;
import com.proautokimium.api.Infrastructure.services.notification.NotificationService;
import com.proautokimium.api.Infrastructure.services.storage.EmployeeDocumentStorageService;
import com.proautokimium.api.domain.entities.Employee;
import com.proautokimium.api.domain.entities.humanResources.EmployeeDocument;
import com.proautokimium.api.domain.enums.NotificationType;
import com.proautokimium.api.domain.exceptions.partners.EmployeeNotFoundException;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
public class EmployeeDocumentService {

    private final EmployeeDocumentRepository repository;
    private final EmployeeRepository employeeRepository;
    private final UserRepository userRepository;
    private final EmployeeDocumentStorageService storage;
    private final NotificationService notificationService;
    private final Clock clock;

    public EmployeeDocumentService(
            EmployeeDocumentRepository repository,
            EmployeeRepository employeeRepository,
            UserRepository userRepository,
            EmployeeDocumentStorageService storage,
            NotificationService notificationService,
            Clock clock
    ) {
        this.repository = repository;
        this.employeeRepository = employeeRepository;
        this.userRepository = userRepository;
        this.storage = storage;
        this.notificationService = notificationService;
        this.clock = clock;
    }

    /** Vincula um documento assinado ao funcionário — só o RH pode chamar isso. */
    @Transactional
    public EmployeeDocumentResponseDTO vincular(UUID employeeId, String title, MultipartFile file) throws IOException {
        Employee employee = employeeRepository.findById(employeeId)
                .orElseThrow(EmployeeNotFoundException::new);

        String storagePath = storage.save(file.getBytes(), employee.getCodParceiro(), file.getOriginalFilename());

        EmployeeDocument document = new EmployeeDocument();
        document.setEmployee(employee);
        document.setTitle(title);
        document.setOriginalFilename(file.getOriginalFilename());
        document.setStoragePath(storagePath);
        document.setUploadedAt(LocalDateTime.now(clock));

        EmployeeDocument saved = repository.save(document);

        notificarFuncionario(employee, title);

        return toResponse(saved);
    }

    /** Lista os documentos do funcionário vinculado ao login autenticado. */
    public List<EmployeeDocumentResponseDTO> listarDoFuncionario(String login) {
        Employee emp = resolveEmployee(login);
        if (emp == null) return List.of();

        return repository.findByEmployeeOrderByUploadedAtDesc(emp).stream()
                .map(this::toResponse)
                .toList();
    }

    public Optional<EmployeeDocument> buscar(UUID id) {
        return repository.findById(id);
    }

    /** Permite o dono do documento ou um usuário de RH/ADMIN. */
    public boolean podeAcessar(EmployeeDocument doc, String login, boolean isRh) {
        if (isRh) return true;
        Employee emp = resolveEmployee(login);
        return emp != null && doc.getEmployee().getId().equals(emp.getId());
    }

    public byte[] lerArquivo(EmployeeDocument doc) throws IOException {
        return Files.readAllBytes(storage.resolve(doc.getStoragePath()));
    }

    private void notificarFuncionario(Employee employee, String title) {
        userRepository.findByEmployee_Id(employee.getId()).ifPresent(user ->
                notificationService.notify(user.getLogin(), NotificationType.DOCUMENTO,
                        "Novo documento disponível",
                        "O RH vinculou um novo documento ao seu cadastro: " + title,
                        "/documentos"));
    }

    private Employee resolveEmployee(String login) {
        Employee viaLink = userRepository.findByLoginWithEmployee(login)
                .map(u -> u.getEmployee())
                .orElse(null);
        if (viaLink != null) return viaLink;
        return employeeRepository.findByUsername(login).orElse(null);
    }

    private EmployeeDocumentResponseDTO toResponse(EmployeeDocument doc) {
        return new EmployeeDocumentResponseDTO(
                doc.getId(), doc.getEmployee().getId(), doc.getTitle(), doc.getOriginalFilename(), doc.getUploadedAt()
        );
    }
}
