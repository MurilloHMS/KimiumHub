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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;

import java.lang.reflect.Field;
import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MedicalCertificateServiceTest {

    @Mock private MedicalCertificateRepository repository;
    @Mock private EmployeeRepository employeeRepository;
    @Mock private UserRepository userRepository;
    @Mock private MedicalCertificateStorageService storage;

    private MedicalCertificateService service;
    private UUID employeeId;
    private Employee employee;

    @BeforeEach
    void setUp() throws Exception {
        Clock clock = Clock.fixed(LocalDateTime.of(2026, 7, 23, 10, 0).atZone(ZoneId.systemDefault()).toInstant(), ZoneId.systemDefault());
        service = new MedicalCertificateService(repository, employeeRepository, userRepository, storage, clock);

        employeeId = UUID.randomUUID();
        employee = new Employee();
        employee.setCodParceiro("EMP001");
        Field field = com.proautokimium.api.domain.abstractions.Entity.class.getDeclaredField("id");
        field.setAccessible(true);
        field.set(employee, employeeId);
    }

    @Test
    @DisplayName("Deve enviar atestado salvando no storage")
    void deveEnviarAtestado() throws Exception {
        MockMultipartFile file = new MockMultipartFile("file", "atestado.pdf", "application/pdf", "conteudo".getBytes());

        when(employeeRepository.findById(employeeId)).thenReturn(Optional.of(employee));
        when(storage.save(any(), eq("EMP001"), eq("atestado.pdf"))).thenReturn("EMP001/uuid-atestado.pdf");
        when(repository.save(any(MedicalCertificate.class))).thenAnswer(inv -> inv.getArgument(0));

        MedicalCertificateResponseDTO response = service.submit(
                employeeId, LocalDate.of(2026, 7, 20), LocalDate.of(2026, 7, 21),
                SubmissionType.FILE, null, file
        );

        assertThat(response.daysCount()).isEqualTo(2);
        assertThat(response.originalFilename()).isEqualTo("atestado.pdf");
    }

    @Test
    @DisplayName("getForRh soma o histórico completo com a contagem só do ano corrente")
    void getForRhRetornaHistoricoEContagemDoAno() {
        when(employeeRepository.findById(employeeId)).thenReturn(Optional.of(employee));
        when(repository.findByEmployeeOrderByStartDateDesc(employee)).thenReturn(List.of());
        when(repository.countByEmployeeAndStartDateBetween(
                employee, LocalDate.of(2026, 1, 1), LocalDate.of(2026, 12, 31))).thenReturn(4L);

        EmployeeMedicalCertificatesDTO result = service.getForRh(employeeId);

        assertThat(result.countThisYear()).isEqualTo(4L);
    }

    @Test
    @DisplayName("RH sempre acessa; dono acessa; terceiro não acessa")
    void controleDeAcesso() {
        MedicalCertificate certificate = MedicalCertificate.submit(
                employee, LocalDate.of(2026, 7, 20), LocalDate.of(2026, 7, 20),
                SubmissionType.FILE, null, "atestado.pdf", "EMP001/atestado.pdf", LocalDateTime.of(2026, 7, 20, 9, 0)
        );

        assertThat(service.podeAcessar(certificate, "qualquer-login", true)).isTrue();

        when(userRepository.findByLoginWithEmployee("dono.login")).thenReturn(Optional.empty());
        when(employeeRepository.findByUsername("dono.login")).thenReturn(Optional.of(employee));
        assertThat(service.podeAcessar(certificate, "dono.login", false)).isTrue();

        Employee outro = new Employee();
        when(userRepository.findByLoginWithEmployee("outro.login")).thenReturn(Optional.empty());
        when(employeeRepository.findByUsername("outro.login")).thenReturn(Optional.of(outro));
        assertThat(service.podeAcessar(certificate, "outro.login", false)).isFalse();
    }
}
