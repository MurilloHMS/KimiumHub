package com.proautokimium.api.Infrastructure.services.humanResources;

import com.proautokimium.api.Application.DTOs.humanResources.Reimbursement.PayReimbursementDTO;
import com.proautokimium.api.Application.DTOs.humanResources.Reimbursement.ReimbursementResponseDTO;
import com.proautokimium.api.Application.DTOs.humanResources.Reimbursement.ReviewReimbursementDTO;
import com.proautokimium.api.Infrastructure.exceptions.humanResources.ReimbursementNotFoundException;
import com.proautokimium.api.Infrastructure.repositories.EmployeeRepository;
import com.proautokimium.api.Infrastructure.repositories.UserRepository;
import com.proautokimium.api.Infrastructure.repositories.humanResources.ReimbursementRepository;
import com.proautokimium.api.Infrastructure.services.notification.NotificationService;
import com.proautokimium.api.Infrastructure.services.storage.ReimbursementStorageService;
import com.proautokimium.api.domain.entities.Employee;
import com.proautokimium.api.domain.entities.humanResources.Reimbursement;
import com.proautokimium.api.domain.enums.NotificationType;
import com.proautokimium.api.domain.enums.humanResources.ReimbursementStatus;
import com.proautokimium.api.domain.exceptions.partners.EmployeeNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
public class ReimbursementService {

    private final ReimbursementRepository repository;
    private final EmployeeRepository employeeRepository;
    private final UserRepository userRepository;
    private final ReimbursementStorageService storage;
    private final NotificationService notificationService;
    private final Clock clock;

    public ReimbursementService(
            ReimbursementRepository repository,
            EmployeeRepository employeeRepository,
            UserRepository userRepository,
            ReimbursementStorageService storage,
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

    @Transactional
    public ReimbursementResponseDTO request(UUID employeeId, LocalDate expenseDate, BigDecimal amount,
                                             String category, String reason, MultipartFile receipt) throws IOException {
        Employee employee = employeeRepository.findById(employeeId)
                .orElseThrow(EmployeeNotFoundException::new);

        String storagePath = storage.save(receipt.getBytes(), employee.getCodParceiro(), receipt.getOriginalFilename());

        Reimbursement reimbursement = Reimbursement.request(
                employee, expenseDate, amount, category, reason,
                receipt.getOriginalFilename(), storagePath, LocalDateTime.now(clock)
        );

        Reimbursement saved = repository.save(reimbursement);
        return toResponse(saved);
    }

    @Transactional
    public ReimbursementResponseDTO approve(UUID id, ReviewReimbursementDTO dto) {
        Reimbursement reimbursement = repository.findById(id).orElseThrow(ReimbursementNotFoundException::new);
        Employee reviewer = employeeRepository.findById(dto.reviewerId()).orElseThrow(EmployeeNotFoundException::new);

        reimbursement.approve(reviewer, dto.notes(), LocalDateTime.now(clock));
        Reimbursement saved = repository.save(reimbursement);

        notificar(saved, "Reembolso aprovado",
                "Seu reembolso de " + saved.getCategory() + " foi aprovado. Aguarde a data de pagamento.");
        return toResponse(saved);
    }

    @Transactional
    public ReimbursementResponseDTO reject(UUID id, ReviewReimbursementDTO dto) {
        Reimbursement reimbursement = repository.findById(id).orElseThrow(ReimbursementNotFoundException::new);
        Employee reviewer = employeeRepository.findById(dto.reviewerId()).orElseThrow(EmployeeNotFoundException::new);

        reimbursement.reject(reviewer, dto.notes(), LocalDateTime.now(clock));
        Reimbursement saved = repository.save(reimbursement);

        notificar(saved, "Reembolso recusado",
                "Seu reembolso de " + saved.getCategory() + " foi recusado. Motivo: " + saved.getReviewNotes());
        return toResponse(saved);
    }

    @Transactional
    public ReimbursementResponseDTO pay(UUID id, PayReimbursementDTO dto) {
        Reimbursement reimbursement = repository.findById(id).orElseThrow(ReimbursementNotFoundException::new);

        reimbursement.pay(dto.paymentDate(), LocalDateTime.now(clock));
        Reimbursement saved = repository.save(reimbursement);

        notificar(saved, "Reembolso pago",
                "Seu reembolso de " + saved.getCategory() + " foi pago em " + saved.getPaymentDate() + ".");
        return toResponse(saved);
    }

    public List<ReimbursementResponseDTO> listByEmployee(UUID employeeId) {
        Employee employee = employeeRepository.findById(employeeId).orElseThrow(EmployeeNotFoundException::new);
        return repository.findByEmployeeOrderByRequestedAtDesc(employee).stream().map(this::toResponse).toList();
    }

    /** Lista os documentos do funcionário vinculado ao login autenticado — "meus reembolsos". */
    public List<ReimbursementResponseDTO> listMine(String login) {
        Employee emp = resolveEmployee(login);
        if (emp == null) return List.of();
        return repository.findByEmployeeOrderByRequestedAtDesc(emp).stream().map(this::toResponse).toList();
    }

    /** Gerenciador do RH — lista tudo, opcionalmente filtrado por status. */
    public List<ReimbursementResponseDTO> listAll(ReimbursementStatus status) {
        List<Reimbursement> results = status != null
                ? repository.findByStatusOrderByRequestedAtDesc(status)
                : repository.findAllByOrderByRequestedAtDesc();
        return results.stream().map(this::toResponse).toList();
    }

    public Optional<Reimbursement> buscar(UUID id) {
        return repository.findById(id);
    }

    /** Permite o dono do reembolso ou um usuário de RH/ADMIN. */
    public boolean podeAcessar(Reimbursement reimbursement, String login, boolean isRh) {
        if (isRh) return true;
        Employee emp = resolveEmployee(login);
        return emp != null && reimbursement.getEmployee().getId().equals(emp.getId());
    }

    public byte[] lerComprovante(Reimbursement reimbursement) throws IOException {
        return Files.readAllBytes(storage.resolve(reimbursement.getReceiptStoragePath()));
    }

    private void notificar(Reimbursement reimbursement, String title, String message) {
        userRepository.findByEmployee_Id(reimbursement.getEmployee().getId()).ifPresent(user ->
                notificationService.notify(user.getLogin(), NotificationType.REEMBOLSO, title, message, "/reembolsos"));
    }

    private Employee resolveEmployee(String login) {
        Employee viaLink = userRepository.findByLoginWithEmployee(login)
                .map(u -> u.getEmployee())
                .orElse(null);
        if (viaLink != null) return viaLink;
        return employeeRepository.findByUsername(login).orElse(null);
    }

    private ReimbursementResponseDTO toResponse(Reimbursement reimbursement) {
        return new ReimbursementResponseDTO(
                reimbursement.getId(),
                reimbursement.getEmployee().getId(),
                reimbursement.getExpenseDate(),
                reimbursement.getAmount(),
                reimbursement.getCategory(),
                reimbursement.getReason(),
                reimbursement.getReceiptOriginalFilename(),
                reimbursement.getStatus(),
                reimbursement.getRequestedAt(),
                reimbursement.getReviewedBy() != null ? reimbursement.getReviewedBy().getId() : null,
                reimbursement.getReviewedAt(),
                reimbursement.getReviewNotes(),
                reimbursement.getPaymentDate(),
                reimbursement.getPaidAt()
        );
    }
}
