package com.proautokimium.api.Infrastructure.services.humanResources;

import com.proautokimium.api.Application.DTOs.humanResources.MedicalCertificate.MedicalCertificateResponseDTO;
import com.proautokimium.api.Application.DTOs.humanResources.MedicalCertificate.ReviewMedicalCertificateDTO;
import com.proautokimium.api.Infrastructure.exceptions.humanResources.MedicalCertificateNotFoundException;
import com.proautokimium.api.Infrastructure.repositories.EmployeeRepository;
import com.proautokimium.api.Infrastructure.repositories.UserRepository;
import com.proautokimium.api.Infrastructure.repositories.humanResources.MedicalCertificateRepository;
import com.proautokimium.api.Infrastructure.services.notification.NotificationService;
import com.proautokimium.api.Infrastructure.services.storage.MedicalCertificateStorageService;
import com.proautokimium.api.domain.entities.Employee;
import com.proautokimium.api.domain.entities.auth.User;
import com.proautokimium.api.domain.entities.humanResources.MedicalCertificate;
import com.proautokimium.api.domain.enums.NotificationType;
import com.proautokimium.api.domain.enums.UserRole;
import com.proautokimium.api.domain.enums.humanResources.MedicalCertificateStatus;
import com.proautokimium.api.domain.enums.humanResources.SubmissionType;
import com.proautokimium.api.domain.exceptions.humanResources.InvalidRequestDataException;
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
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/**
 * O que a conferência promete a cada lado: quem enviou fica sabendo do que o RH
 * decidiu, e o RH fica sabendo de cada envio — o primeiro e cada reenvio.
 */
@ExtendWith(MockitoExtension.class)
class MedicalCertificateReviewServiceTest {

    private static final LocalDateTime AGORA = LocalDateTime.of(2026, 10, 2, 10, 0);

    @Mock MedicalCertificateRepository repository;
    @Mock EmployeeRepository employeeRepository;
    @Mock UserRepository userRepository;
    @Mock MedicalCertificateStorageService storage;
    @Mock NotificationService notificationService;

    MedicalCertificateService service;
    Employee ana;
    Employee rita;

    @BeforeEach
    void setUp() {
        Clock clock = Clock.fixed(AGORA.atZone(ZoneId.systemDefault()).toInstant(), ZoneId.systemDefault());
        service = new MedicalCertificateService(repository, employeeRepository, userRepository, storage,
                notificationService, clock);
        ana = employee("Ana", "9001");
        rita = employee("Rita", "9002");
    }

    private MedicalCertificate atestadoDaAna() {
        MedicalCertificate c = MedicalCertificate.submit(ana, LocalDate.of(2026, 9, 29), LocalDate.of(2026, 9, 30),
                SubmissionType.FILE, null, "a.pdf", "9001/a.pdf", AGORA.minusDays(1));
        when(repository.findById(any())).thenReturn(Optional.of(c));
        return c;
    }

    private void logado(String login, Employee e) {
        User u = new User(login, login + "@t.com", "x", List.of(UserRole.USER));
        u.setEmployee(e);
        when(userRepository.findByLoginWithEmployee(login)).thenReturn(Optional.of(u));
    }

    private void anaTemLogin() {
        User u = new User("ana", "ana@t.com", "x", List.of(UserRole.USER));
        when(userRepository.findByEmployee_Id(ana.getId())).thenReturn(Optional.of(u));
    }

    @Test
    @DisplayName("confirmar avisa quem enviou que o RH recebeu")
    void confirmNotifiesOwner() {
        atestadoDaAna();
        logado("rita", rita);
        anaTemLogin();
        when(repository.save(any())).thenAnswer(i -> i.getArgument(0));

        MedicalCertificateResponseDTO r = service.confirmReceipt(UUID.randomUUID(), null, "rita");

        assertThat(r.status()).isEqualTo(MedicalCertificateStatus.RECEIVED);
        assertThat(r.reviewedByName()).isEqualTo("Rita");
        verify(notificationService).notify(eq("ana"), eq(NotificationType.ATESTADO), eq("Atestado recebido"),
                contains("29/09 a 30/09"), eq("/documentos/rh/medical-certificates"));
    }

    @Test
    @DisplayName("recusar avisa com o motivo, e a resposta diz até quando dá para reenviar")
    void rejectNotifiesWithReason() {
        atestadoDaAna();
        logado("rita", rita);
        anaTemLogin();
        when(repository.save(any())).thenAnswer(i -> i.getArgument(0));

        MedicalCertificateResponseDTO r = service.reject(UUID.randomUUID(),
                new ReviewMedicalCertificateDTO("Ilegível"), "rita");

        assertThat(r.status()).isEqualTo(MedicalCertificateStatus.REJECTED);
        assertThat(r.resubmitDeadline()).isEqualTo(AGORA.plusDays(30));
        verify(notificationService).notify(eq("ana"), eq(NotificationType.ATESTADO), eq("Atestado recusado"),
                contains("Motivo: Ilegível"), anyString());
    }

    @Test
    @DisplayName("o envio avisa quem confere atestados, menos quem enviou")
    void submitNotifiesReviewers() throws Exception {
        logado("ana", ana);
        when(storage.save(any(), eq("9001"), eq("a.pdf"))).thenReturn("9001/a.pdf");
        when(repository.save(any())).thenAnswer(i -> i.getArgument(0));
        when(userRepository.findActiveLoginsAllowed("rh/medical-certificates", "ALTERAR"))
                .thenReturn(List.of("rita", "ana", "bruno"));

        service.submit("ana", LocalDate.of(2026, 9, 29), LocalDate.of(2026, 9, 29), SubmissionType.FILE, null,
                new MockMultipartFile("file", "a.pdf", "application/pdf", new byte[]{1}));

        verify(notificationService).notify(eq("rita"), eq(NotificationType.ATESTADO), eq("Atestado para conferir"),
                eq("Ana enviou um atestado de 29/09."), eq("/rh/medical-certificates"));
        verify(notificationService).notify(eq("bruno"), any(), any(), any(), any());
        verify(notificationService, never()).notify(eq("ana"), any(), any(), any(), any());
    }

    @Test
    @DisplayName("o sino falhar não desfaz o envio")
    void notificationFailureDoesNotBreak() throws Exception {
        logado("ana", ana);
        when(storage.save(any(), any(), any())).thenReturn("9001/a.pdf");
        when(repository.save(any())).thenAnswer(i -> i.getArgument(0));
        when(userRepository.findActiveLoginsAllowed(any(), any())).thenReturn(List.of("rita"));
        doThrow(new RuntimeException("push fora")).when(notificationService)
                .notify(any(), any(), any(), any(), any());

        MedicalCertificateResponseDTO r = service.submit("ana", LocalDate.of(2026, 9, 29),
                LocalDate.of(2026, 9, 29), SubmissionType.FILE, null,
                new MockMultipartFile("file", "a.pdf", "application/pdf", new byte[]{1}));

        assertThat(r.status()).isEqualTo(MedicalCertificateStatus.PENDING);
        verify(storage, never()).delete(any());
    }

    @Test
    @DisplayName("reenviar troca o arquivo e avisa o RH de novo")
    void resubmitNotifiesReviewers() throws Exception {
        MedicalCertificate c = atestadoDaAna();
        c.reject(rita, "Ilegível", AGORA.minusHours(2));
        logado("ana", ana);
        when(storage.save(any(), eq("9001"), eq("b.pdf"))).thenReturn("9001/b.pdf");
        when(repository.save(any())).thenAnswer(i -> i.getArgument(0));
        when(userRepository.findActiveLoginsAllowed(any(), any())).thenReturn(List.of("rita"));

        MedicalCertificateResponseDTO r = service.resubmit(UUID.randomUUID(), "ana", SubmissionType.FILE, null,
                "Mais nítido", new MockMultipartFile("file", "b.pdf", "application/pdf", new byte[]{1}));

        assertThat(r.status()).isEqualTo(MedicalCertificateStatus.PENDING);
        assertThat(r.originalFilename()).isEqualTo("b.pdf");
        assertThat(r.previousAttempts()).singleElement().satisfies(a -> {
            assertThat(a.originalFilename()).isEqualTo("a.pdf");
            assertThat(a.reviewNotes()).isEqualTo("Ilegível");
            assertThat(a.reviewedByName()).isEqualTo("Rita");
        });
        verify(notificationService).notify(eq("rita"), eq(NotificationType.ATESTADO), eq("Atestado reenviado"),
                contains("Ana reenviou"), eq("/rh/medical-certificates"));
    }

    @Test
    @DisplayName("quem não é o dono recebe 404, e nenhum arquivo é gravado")
    void resubmitByOtherIsNotFound() throws Exception {
        MedicalCertificate c = atestadoDaAna();
        c.reject(rita, "Ilegível", AGORA.minusHours(2));
        logado("bruno", employee("Bruno", "9003"));

        assertThrows(MedicalCertificateNotFoundException.class, () -> service.resubmit(UUID.randomUUID(), "bruno",
                SubmissionType.FILE, null, null, new MockMultipartFile("file", "b.pdf", "application/pdf", new byte[]{1})));

        verify(storage, never()).save(any(), any(), any());
        assertThat(c.getStatus()).isEqualTo(MedicalCertificateStatus.REJECTED);
    }

    @Test
    @DisplayName("reenvio recusado pela regra apaga o arquivo que acabou de ser gravado")
    void refusedResubmitDeletesFile() throws Exception {
        atestadoDaAna(); // ainda em conferência: não se reenvia
        logado("ana", ana);
        when(storage.save(any(), any(), any())).thenReturn("9001/b.pdf");

        assertThrows(RuntimeException.class, () -> service.resubmit(UUID.randomUUID(), "ana",
                SubmissionType.FILE, null, null, new MockMultipartFile("file", "b.pdf", "application/pdf", new byte[]{1})));

        verify(storage).delete("9001/b.pdf");
        verify(repository, never()).save(any());
        verifyNoInteractions(notificationService);
    }

    @Test
    @DisplayName("reenvio sem arquivo é recusado antes de gravar")
    void resubmitWithoutFile() {
        atestadoDaAna();
        logado("ana", ana);

        assertThrows(InvalidRequestDataException.class, () -> service.resubmit(UUID.randomUUID(), "ana",
                SubmissionType.FILE, null, null, new MockMultipartFile("file", "b.pdf", "application/pdf", new byte[0])));
        verifyNoInteractions(storage);
    }

    private static Employee employee(String name, String cod) {
        Employee e = new Employee();
        e.setName(name);
        e.setCodParceiro(cod);
        try {
            Field f = com.proautokimium.api.domain.abstractions.Entity.class.getDeclaredField("id");
            f.setAccessible(true);
            f.set(e, UUID.randomUUID());
        } catch (ReflectiveOperationException ex) {
            throw new IllegalStateException(ex);
        }
        return e;
    }
}
