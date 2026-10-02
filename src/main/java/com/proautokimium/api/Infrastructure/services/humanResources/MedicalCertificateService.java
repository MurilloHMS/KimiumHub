package com.proautokimium.api.Infrastructure.services.humanResources;

import com.proautokimium.api.Application.DTOs.humanResources.MedicalCertificate.EmployeeMedicalCertificatesDTO;
import com.proautokimium.api.Application.DTOs.humanResources.MedicalCertificate.MedicalCertificateAttemptDTO;
import com.proautokimium.api.Application.DTOs.humanResources.MedicalCertificate.ReviewMedicalCertificateDTO;
import com.proautokimium.api.Infrastructure.exceptions.humanResources.MedicalCertificateNotFoundException;
import com.proautokimium.api.Application.DTOs.humanResources.MedicalCertificate.MedicalCertificateResponseDTO;
import com.proautokimium.api.Infrastructure.repositories.EmployeeRepository;
import com.proautokimium.api.Infrastructure.repositories.UserRepository;
import com.proautokimium.api.Infrastructure.repositories.humanResources.MedicalCertificateRepository;
import com.proautokimium.api.Infrastructure.services.notification.NotificationService;
import com.proautokimium.api.Infrastructure.services.storage.MedicalCertificateStorageService;
import com.proautokimium.api.domain.entities.Employee;
import com.proautokimium.api.domain.entities.humanResources.MedicalCertificate;
import com.proautokimium.api.domain.entities.humanResources.MedicalCertificateAttempt;
import com.proautokimium.api.domain.enums.NotificationType;
import com.proautokimium.api.domain.enums.humanResources.MedicalCertificateStatus;
import com.proautokimium.api.domain.exceptions.humanResources.InvalidRequestDataException;
import com.proautokimium.api.domain.enums.humanResources.SubmissionType;
import com.proautokimium.api.domain.exceptions.partners.EmployeeNotFoundException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Slf4j
@Service
public class MedicalCertificateService {

    /** Quem confere: a célula de ação da tela do RH. */
    static final String REVIEW_SCREEN = "rh/medical-certificates";
    static final String REVIEW_PERMISSION = "ALTERAR";
    static final String EMPLOYEE_LINK = "/documentos/rh/medical-certificates";
    static final String RH_LINK = "/rh/medical-certificates";

    private static final DateTimeFormatter DIA_MES = DateTimeFormatter.ofPattern("dd/MM");

    private final MedicalCertificateRepository repository;
    private final EmployeeRepository employeeRepository;
    private final UserRepository userRepository;
    private final MedicalCertificateStorageService storage;
    private final NotificationService notificationService;
    private final Clock clock;

    public MedicalCertificateService(
            MedicalCertificateRepository repository,
            EmployeeRepository employeeRepository,
            UserRepository userRepository,
            MedicalCertificateStorageService storage,
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

    /** Quem envia é sempre o funcionário autenticado — employeeId nunca vem do cliente. */
    @Transactional
    public MedicalCertificateResponseDTO submit(String login, LocalDate startDate, LocalDate endDate,
                                                 SubmissionType submissionType, Boolean confirmedLegible,
                                                 MultipartFile file) throws IOException {
        Employee employee = resolveEmployee(login);
        if (employee == null) {
            throw new EmployeeNotFoundException();
        }

        String storagePath = storage.save(file.getBytes(), employee.getCodParceiro(), file.getOriginalFilename());

        // A entidade precisa do caminho, então o arquivo é salvo antes de ela
        // validar. Recusado o envio, o arquivo é apagado — senão fica órfão.
        try {
            MedicalCertificate certificate = MedicalCertificate.submit(
                    employee, startDate, endDate, submissionType, confirmedLegible,
                    file.getOriginalFilename(), storagePath, LocalDateTime.now(clock)
            );

            MedicalCertificate saved = repository.save(certificate);
            notifyReviewers(saved, login, "Atestado para conferir",
                    employee.getName() + " enviou um atestado de " + period(saved) + ".");
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

    /** Histórico do funcionário vinculado ao login autenticado — "meus atestados". */
    public List<MedicalCertificateResponseDTO> listMine(String login) {
        Employee emp = resolveEmployee(login);
        if (emp == null) return List.of();
        return repository.findByEmployeeOrderByStartDateDesc(emp).stream().map(this::toResponse).toList();
    }

    /** RH: histórico completo + contagem no ano corrente. */
    public EmployeeMedicalCertificatesDTO getForRh(UUID employeeId) {
        Employee employee = employeeRepository.findById(employeeId)
                .orElseThrow(EmployeeNotFoundException::new);

        List<MedicalCertificateResponseDTO> history = repository.findByEmployeeOrderByStartDateDesc(employee).stream()
                .map(this::toResponse)
                .toList();

        int currentYear = LocalDate.now(clock).getYear();
        long countThisYear = repository.countByEmployeeAndStatusNotAndStartDateBetween(
                employee, MedicalCertificateStatus.REJECTED,
                LocalDate.of(currentYear, 1, 1), LocalDate.of(currentYear, 12, 31));

        return new EmployeeMedicalCertificatesDTO(history, countThisYear);
    }

    public List<MedicalCertificateResponseDTO> listAll() {
        return listAll(null);
    }

    /** Gerenciador do RH — tudo, ou só um status. */
    public List<MedicalCertificateResponseDTO> listAll(MedicalCertificateStatus status) {
        List<MedicalCertificate> results = status == null
                ? repository.findAllByOrderBySubmittedAtDesc()
                : repository.findByStatusOrderBySubmittedAtDesc(status);
        return results.stream().map(this::toResponse).toList();
    }

    /** O RH confirma que recebeu; quem enviou é avisado. */
    @Transactional
    public MedicalCertificateResponseDTO confirmReceipt(UUID id, ReviewMedicalCertificateDTO dto, String reviewerLogin) {
        MedicalCertificate certificate = repository.findById(id).orElseThrow(MedicalCertificateNotFoundException::new);
        Employee reviewer = resolveEmployee(reviewerLogin);
        if (reviewer == null) {
            throw new EmployeeNotFoundException();
        }

        certificate.confirmReceipt(reviewer, dto == null ? null : dto.notes(), LocalDateTime.now(clock));
        MedicalCertificate saved = repository.save(certificate);

        notifyOwner(saved, "Atestado recebido",
                "O RH confirmou o recebimento do seu atestado de " + period(saved) + ".");
        return toResponse(saved);
    }

    /** O RH recusa com o motivo; quem enviou é avisado e pode reenviar. */
    @Transactional
    public MedicalCertificateResponseDTO reject(UUID id, ReviewMedicalCertificateDTO dto, String reviewerLogin) {
        MedicalCertificate certificate = repository.findById(id).orElseThrow(MedicalCertificateNotFoundException::new);
        Employee reviewer = resolveEmployee(reviewerLogin);
        if (reviewer == null) {
            throw new EmployeeNotFoundException();
        }

        certificate.reject(reviewer, dto == null ? null : dto.notes(), LocalDateTime.now(clock));
        MedicalCertificate saved = repository.save(certificate);

        notifyOwner(saved, "Atestado recusado",
                "O RH recusou seu atestado de " + period(saved) + ". Motivo: " + saved.getReviewNotes()
                        + ". Você pode enviar outro arquivo.");
        return toResponse(saved);
    }

    /**
     * O dono reenvia o atestado recusado com um arquivo novo.
     *
     * Quem não é o dono recebe o mesmo 404 de "não existe": responder 403
     * confirmaria que aquele id é um atestado de outra pessoa.
     *
     * O arquivo é salvo antes de a entidade validar (ela precisa do caminho), e
     * apagado se o reenvio for recusado — o mesmo cuidado do {@link #submit}.
     */
    @Transactional
    public MedicalCertificateResponseDTO resubmit(UUID id, String login, SubmissionType submissionType,
                                                  Boolean confirmedLegible, String comment, MultipartFile file)
            throws IOException {
        MedicalCertificate certificate = repository.findById(id).orElseThrow(MedicalCertificateNotFoundException::new);
        Employee caller = resolveEmployee(login);
        if (caller == null || !isOwner(certificate, caller)) {
            throw new MedicalCertificateNotFoundException();
        }
        if (file == null || file.isEmpty()) {
            throw new InvalidRequestDataException("Anexe o novo atestado");
        }

        String storagePath = storage.save(file.getBytes(), caller.getCodParceiro(), file.getOriginalFilename());
        try {
            certificate.resubmit(submissionType, confirmedLegible, file.getOriginalFilename(), storagePath,
                    comment, LocalDateTime.now(clock));
            MedicalCertificate saved = repository.save(certificate);
            notifyReviewers(saved, login, "Atestado reenviado",
                    caller.getName() + " reenviou o atestado de " + period(saved) + " depois da recusa.");
            return toResponse(saved);
        } catch (RuntimeException refused) {
            try {
                storage.delete(storagePath);
            } catch (IOException deleteFailure) {
                refused.addSuppressed(deleteFailure);
            }
            throw refused;
        }
    }

    /** Um arquivo recusado da trilha — o que a conferência daquela vez viu. */
    public Optional<MedicalCertificateAttempt> findAttempt(MedicalCertificate certificate, UUID attemptId) {
        return certificate.getPreviousAttempts().stream()
                .filter(a -> attemptId.equals(a.getId()))
                .findFirst();
    }

    public byte[] readAttemptFile(MedicalCertificateAttempt attempt) throws IOException {
        return Files.readAllBytes(storage.resolve(attempt.getStoragePath()));
    }

    public Optional<MedicalCertificate> buscar(UUID id) {
        return repository.findById(id);
    }

    /** Permite o dono do atestado ou um usuário de RH/ADMIN. */
    public boolean podeAcessar(MedicalCertificate certificate, String login, boolean isRh) {
        if (isRh) return true;
        Employee emp = resolveEmployee(login);
        return emp != null && certificate.getEmployee().getId().equals(emp.getId());
    }

    public byte[] lerArquivo(MedicalCertificate certificate) throws IOException {
        return Files.readAllBytes(storage.resolve(certificate.getStoragePath()));
    }

    private void notifyOwner(MedicalCertificate certificate, String title, String message) {
        userRepository.findByEmployee_Id(certificate.getEmployee().getId()).ifPresent(user ->
                safely(() -> notificationService.notify(user.getLogin(), NotificationType.ATESTADO, title, message,
                        EMPLOYEE_LINK)));
    }

    /**
     * Avisa quem confere atestados — menos quem enviou: a pessoa do RH que
     * manda o próprio atestado não precisa de sino sobre ele, e nem pode
     * conferi-lo.
     */
    private void notifyReviewers(MedicalCertificate certificate, String senderLogin, String title, String message) {
        for (String login : userRepository.findActiveLoginsAllowed(REVIEW_SCREEN, REVIEW_PERMISSION)) {
            if (login.equals(senderLogin)) continue;
            safely(() -> notificationService.notify(login, NotificationType.ATESTADO, title, message, RH_LINK));
        }
    }

    /** O aviso é melhor esforço: o atestado foi gravado, e isso não se desfaz porque o sino falhou. */
    private void safely(Runnable send) {
        try {
            send.run();
        } catch (RuntimeException e) {
            log.warn("Falha ao avisar sobre atestado", e);
        }
    }

    private static String period(MedicalCertificate c) {
        return c.getStartDate().equals(c.getEndDate())
                ? c.getStartDate().format(DIA_MES)
                : c.getStartDate().format(DIA_MES) + " a " + c.getEndDate().format(DIA_MES);
    }

    private static boolean isOwner(MedicalCertificate certificate, Employee caller) {
        Employee owner = certificate.getEmployee();
        return owner == caller || (caller.getId() != null && caller.getId().equals(owner.getId()));
    }

    private Employee resolveEmployee(String login) {
        Employee viaLink = userRepository.findByLoginWithEmployee(login)
                .map(u -> u.getEmployee())
                .orElse(null);
        if (viaLink != null) return viaLink;
        return employeeRepository.findByUsername(login).orElse(null);
    }

    private MedicalCertificateResponseDTO toResponse(MedicalCertificate certificate) {
        return new MedicalCertificateResponseDTO(
                certificate.getId(),
                certificate.getEmployee().getId(),
                certificate.getEmployee().getName(),
                certificate.getStartDate(),
                certificate.getEndDate(),
                certificate.getDaysCount(),
                certificate.getSubmissionType(),
                certificate.getConfirmedLegible(),
                certificate.getOriginalFilename(),
                certificate.getSubmittedAt(),
                certificate.getStatus(),
                nameOf(certificate.getReviewedBy()),
                certificate.getReviewedAt(),
                certificate.getReviewNotes(),
                certificate.getResubmittedAt(),
                certificate.getResubmitComment(),
                certificate.canResubmit(LocalDateTime.now(clock)) ? certificate.resubmitDeadline() : null,
                certificate.getPreviousAttempts().stream().map(a -> new MedicalCertificateAttemptDTO(
                        a.getId(), a.getSubmissionType(), a.getOriginalFilename(), a.getSubmittedAt(),
                        a.getComment(), nameOf(a.getReviewedBy()), a.getReviewedAt(), a.getReviewNotes())).toList()
        );
    }

    private static String nameOf(Employee e) {
        return e == null ? null : e.getName();
    }
}
