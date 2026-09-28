package com.proautokimium.api.Infrastructure.services.humanResources;

import com.proautokimium.api.Application.DTOs.humanResources.Reimbursement.PayReimbursementDTO;
import com.proautokimium.api.Application.DTOs.humanResources.Reimbursement.ReimbursementResponseDTO;
import com.proautokimium.api.Application.DTOs.humanResources.Reimbursement.ReimbursementSummaryDTO;
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
import com.proautokimium.api.domain.exceptions.humanResources.InvalidRequestDataException;
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
import java.time.YearMonth;
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

    /** Quem solicita é sempre o funcionário autenticado — employeeId nunca vem do cliente. */
    @Transactional
    public ReimbursementResponseDTO request(String login, LocalDate expenseDate, BigDecimal amount,
                                             String category, String reason, MultipartFile receipt) throws IOException {
        Employee employee = resolveEmployee(login);
        if (employee == null) {
            throw new EmployeeNotFoundException();
        }

        String storagePath = storage.save(receipt.getBytes(), employee.getCodParceiro(), receipt.getOriginalFilename());

        // A entidade precisa do caminho, então o arquivo é salvo antes de ela
        // validar. Recusado o pedido, o arquivo é apagado — senão fica órfão.
        try {
            Reimbursement reimbursement = Reimbursement.request(
                    employee, expenseDate, amount, category, reason,
                    receipt.getOriginalFilename(), storagePath, LocalDateTime.now(clock)
            );

            Reimbursement saved = repository.save(reimbursement);
            return toResponse(saved);
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

    @Transactional
    public ReimbursementResponseDTO approve(UUID id, ReviewReimbursementDTO dto, String reviewerLogin) {
        Reimbursement reimbursement = repository.findById(id).orElseThrow(ReimbursementNotFoundException::new);
        Employee reviewer = resolveEmployee(reviewerLogin);
        if (reviewer == null) {
            throw new EmployeeNotFoundException();
        }

        reimbursement.approve(reviewer, dto.notes(), LocalDateTime.now(clock));
        Reimbursement saved = repository.save(reimbursement);

        notificar(saved, "Reembolso aprovado",
                "Seu reembolso de " + saved.getCategory() + " foi aprovado. Aguarde a data de pagamento.");
        return toResponse(saved);
    }

    @Transactional
    public ReimbursementResponseDTO reject(UUID id, ReviewReimbursementDTO dto, String reviewerLogin) {
        Reimbursement reimbursement = repository.findById(id).orElseThrow(ReimbursementNotFoundException::new);
        Employee reviewer = resolveEmployee(reviewerLogin);
        if (reviewer == null) {
            throw new EmployeeNotFoundException();
        }

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
        return listAll(status, null);
    }

    /** Com mês, recorta pela data da despesa — o mesmo recorte dos totais da tela. */
    public List<ReimbursementResponseDTO> listAll(ReimbursementStatus status, YearMonth month) {
        List<Reimbursement> results;
        if (month == null) {
            results = status != null
                    ? repository.findByStatusOrderByRequestedAtDesc(status)
                    : repository.findAllByOrderByRequestedAtDesc();
        } else {
            results = status != null
                    ? repository.findByStatusAndExpenseDateBetweenOrderByRequestedAtDesc(
                            status, month.atDay(1), month.atEndOfMonth())
                    : repository.findByExpenseDateBetweenOrderByRequestedAtDesc(month.atDay(1), month.atEndOfMonth());
        }
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

    /**
     * O dono contesta a recusa com um comprovante novo e um comentário.
     *
     * Quem não é o dono recebe o mesmo 404 de "não existe": responder 403
     * confirmaria que aquele id é um reembolso de outra pessoa.
     *
     * O arquivo é salvo antes de a entidade validar (ela precisa do caminho), e
     * apagado se a contestação for recusada — o mesmo cuidado do {@link #request}.
     */
    @Transactional
    public ReimbursementResponseDTO contest(UUID id, String login, String comment, MultipartFile receipt)
            throws IOException {
        Reimbursement reimbursement = repository.findById(id).orElseThrow(ReimbursementNotFoundException::new);
        Employee caller = resolveEmployee(login);
        if (caller == null || !isOwner(reimbursement, caller)) {
            throw new ReimbursementNotFoundException();
        }
        if (receipt == null || receipt.isEmpty()) {
            throw new InvalidRequestDataException("Anexe o novo comprovante");
        }

        String storagePath = storage.save(receipt.getBytes(), caller.getCodParceiro(), receipt.getOriginalFilename());
        try {
            reimbursement.contest(receipt.getOriginalFilename(), storagePath, comment, LocalDateTime.now(clock));
            return toResponse(repository.save(reimbursement));
        } catch (RuntimeException refused) {
            try {
                storage.delete(storagePath);
            } catch (IOException deleteFailure) {
                refused.addSuppressed(deleteFailure);
            }
            throw refused;
        }
    }

    /** O comprovante de antes da contestação — o que a primeira análise viu. */
    public byte[] lerComprovanteOriginal(Reimbursement reimbursement) throws IOException {
        if (reimbursement.getOriginalReceiptStoragePath() == null) {
            throw new ReimbursementNotFoundException();
        }
        return Files.readAllBytes(storage.resolve(reimbursement.getOriginalReceiptStoragePath()));
    }

    /** Totais do mês para o RH: todos os funcionários. */
    public ReimbursementSummaryDTO summary(YearMonth month) {
        return summarize(month, repository.findByExpenseDateBetween(month.atDay(1), month.atEndOfMonth()));
    }

    /** Totais do mês do funcionário autenticado. */
    public ReimbursementSummaryDTO summaryMine(String login, YearMonth month) {
        Employee employee = resolveEmployee(login);
        if (employee == null) {
            throw new EmployeeNotFoundException();
        }
        return summarize(month, repository.findByEmployeeAndExpenseDateBetween(
                employee, month.atDay(1), month.atEndOfMonth()));
    }

    private static ReimbursementSummaryDTO summarize(YearMonth month, List<Reimbursement> items) {
        return new ReimbursementSummaryDTO(
                month.toString(),
                bucket(items),
                bucket(items.stream().filter(r -> r.getStatus() == ReimbursementStatus.PENDING).toList()),
                bucket(items.stream().filter(r -> r.getStatus() == ReimbursementStatus.APPROVED).toList()),
                bucket(items.stream().filter(r -> r.getStatus() == ReimbursementStatus.PAID).toList()),
                items.stream().filter(r -> r.getStatus() == ReimbursementStatus.PENDING
                        && r.getContestedAt() != null).count());
    }

    private static ReimbursementSummaryDTO.Bucket bucket(List<Reimbursement> items) {
        return new ReimbursementSummaryDTO.Bucket(
                items.stream().map(Reimbursement::getAmount).reduce(BigDecimal.ZERO, BigDecimal::add),
                items.size());
    }

    private static boolean isOwner(Reimbursement reimbursement, Employee caller) {
        Employee owner = reimbursement.getEmployee();
        return owner == caller || (caller.getId() != null && caller.getId().equals(owner.getId()));
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
                reimbursement.getPaidAt(),
                reimbursement.getContestedAt(),
                reimbursement.getContestComment(),
                reimbursement.getOriginalReceiptFilename(),
                reimbursement.getFirstReviewedBy() != null ? reimbursement.getFirstReviewedBy().getId() : null,
                reimbursement.getFirstReviewedAt(),
                reimbursement.getFirstReviewNotes(),
                reimbursement.canContest(LocalDateTime.now(clock)) ? reimbursement.contestDeadline() : null
        );
    }
}
