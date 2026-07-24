package com.proautokimium.api.Infrastructure.services.humanResources;

import com.proautokimium.api.Application.DTOs.humanResources.MedicalCertificate.EmployeeMedicalCertificatesDTO;
import com.proautokimium.api.Application.DTOs.humanResources.MedicalCertificate.MedicalCertificateResponseDTO;
import com.proautokimium.api.Infrastructure.repositories.EmployeeRepository;
import com.proautokimium.api.Infrastructure.repositories.UserRepository;
import com.proautokimium.api.Infrastructure.repositories.humanResources.MedicalCertificateRepository;
import com.proautokimium.api.Infrastructure.services.storage.MedicalCertificateStorageService;
import com.proautokimium.api.domain.entities.Employee;
import com.proautokimium.api.domain.entities.humanResources.MedicalCertificate;
import com.proautokimium.api.domain.enums.humanResources.SubmissionType;
import com.proautokimium.api.domain.exceptions.partners.EmployeeNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
public class MedicalCertificateService {

    private final MedicalCertificateRepository repository;
    private final EmployeeRepository employeeRepository;
    private final UserRepository userRepository;
    private final MedicalCertificateStorageService storage;
    private final Clock clock;

    public MedicalCertificateService(
            MedicalCertificateRepository repository,
            EmployeeRepository employeeRepository,
            UserRepository userRepository,
            MedicalCertificateStorageService storage,
            Clock clock
    ) {
        this.repository = repository;
        this.employeeRepository = employeeRepository;
        this.userRepository = userRepository;
        this.storage = storage;
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

        MedicalCertificate certificate = MedicalCertificate.submit(
                employee, startDate, endDate, submissionType, confirmedLegible,
                file.getOriginalFilename(), storagePath, LocalDateTime.now(clock)
        );

        MedicalCertificate saved = repository.save(certificate);
        return toResponse(saved);
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
        long countThisYear = repository.countByEmployeeAndStartDateBetween(
                employee, LocalDate.of(currentYear, 1, 1), LocalDate.of(currentYear, 12, 31));

        return new EmployeeMedicalCertificatesDTO(history, countThisYear);
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
                certificate.getStartDate(),
                certificate.getEndDate(),
                certificate.getDaysCount(),
                certificate.getSubmissionType(),
                certificate.getConfirmedLegible(),
                certificate.getOriginalFilename(),
                certificate.getSubmittedAt()
        );
    }
}
