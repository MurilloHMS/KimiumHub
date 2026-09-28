package com.proautokimium.api.Infrastructure.services.humanResources;

import com.proautokimium.api.Application.DTOs.humanResources.Reimbursement.ReportEmailResultDTO;
import com.proautokimium.api.Infrastructure.exceptions.humanResources.NoReportRecipientException;
import com.proautokimium.api.Infrastructure.exceptions.humanResources.ReportEmailFailedException;
import com.proautokimium.api.Infrastructure.exceptions.humanResources.ReportTooLargeForEmailException;
import com.proautokimium.api.Infrastructure.repositories.EmployeeRepository;
import com.proautokimium.api.Infrastructure.services.email.EmailQueueService;
import com.proautokimium.api.Infrastructure.services.email.smtp.SmtpService;
import com.proautokimium.api.domain.enums.humanResources.ReimbursementStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.thymeleaf.spring6.SpringTemplateEngine;
import org.thymeleaf.templateresolver.ClassLoaderTemplateResolver;

import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;

import static com.proautokimium.api.domain.enums.humanResources.ReimbursementStatus.APPROVED;
import static com.proautokimium.api.domain.enums.humanResources.ReimbursementStatus.PAID;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/** O envio ao RH. O template do e-mail é renderizado de verdade (Thymeleaf). */
@ExtendWith(MockitoExtension.class)
class ReimbursementReportEmailServiceTest {

    private static final LocalDate FROM = LocalDate.of(2026, 9, 1);
    private static final LocalDate TO = LocalDate.of(2026, 9, 30);
    private static final List<ReimbursementStatus> STATUS = List.of(PAID, APPROVED);
    private static final byte[] PDF = {'%', 'P', 'D', 'F'};

    @Mock HrReportRecipientService recipients;
    @Mock ReimbursementReportService reportService;
    @Mock EmailQueueService emailQueueService;
    @Mock EmployeeRepository employeeRepository;

    private ReimbursementReportEmailService service;

    @BeforeEach
    void setUp() {
        ClassLoaderTemplateResolver resolver = new ClassLoaderTemplateResolver();
        resolver.setPrefix("templates/");
        resolver.setSuffix(".html");
        resolver.setCharacterEncoding("UTF-8");
        // O de produção: o TemplateEngine puro avalia com OGNL, que o projeto não tem.
        SpringTemplateEngine engine = new SpringTemplateEngine();
        engine.setTemplateResolver(resolver);
        Clock clock = Clock.fixed(LocalDateTime.of(2026, 9, 28, 14, 32)
                .atZone(ZoneId.of("America/Sao_Paulo")).toInstant(), ZoneId.of("America/Sao_Paulo"));
        service = new ReimbursementReportEmailService(recipients, reportService, emailQueueService,
                employeeRepository, engine, clock, "no-reply@envios.proautokimium.com.br");
    }

    /** Montar um PDF com anexos para descobrir que não há para quem mandar é trabalho jogado fora. */
    @Test
    @DisplayName("sem destinatário cadastrado, recusa antes de gerar o PDF")
    void semDestinatario() {
        when(recipients.emails()).thenReturn(List.of());

        assertThrows(NoReportRecipientException.class,
                () -> service.sendToHr(FROM, TO, STATUS, null, "carla.rh"));
        verifyNoInteractions(reportService, emailQueueService);
    }

    @Test
    @DisplayName("manda um e-mail por destinatário, com o PDF anexado e o filtro no corpo")
    void mandaParaCadaUm() {
        when(recipients.emails()).thenReturn(List.of("rh@proautokimium.com.br", "diretoria@proautokimium.com.br"));
        when(reportService.generate(FROM, TO, STATUS, null, "carla.rh")).thenReturn(PDF);
        when(reportService.issuerName("carla.rh")).thenReturn("Carla Mendes");

        ReportEmailResultDTO result = service.sendToHr(FROM, TO, STATUS, null, "carla.rh");

        ArgumentCaptor<String> corpo = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<SmtpService.Attachment> anexo = ArgumentCaptor.forClass(SmtpService.Attachment.class);
        verify(emailQueueService).sendNow(eq("rh@proautokimium.com.br"), eq("no-reply@envios.proautokimium.com.br"),
                eq("Comprovante de reembolsos · 01/09/2026 a 30/09/2026"), corpo.capture(), anexo.capture());
        verify(emailQueueService).sendNow(eq("diretoria@proautokimium.com.br"), anyString(), anyString(),
                anyString(), any());
        assertThat(anexo.getValue().fileName()).isEqualTo("comprovante-reembolsos_2026-09-01_2026-09-30.pdf");
        assertThat(anexo.getValue().content()).isEqualTo(PDF);
        assertThat(anexo.getValue().contentType()).isEqualTo("application/pdf");
        assertThat(corpo.getValue()).contains("de todos os funcionários", "01/09/2026 a 30/09/2026",
                "Aprovado, Pago", "Carla Mendes", "28/09/2026 14:32");
        assertThat(result.sentTo()).containsExactly("rh@proautokimium.com.br", "diretoria@proautokimium.com.br");
        assertThat(result.failed()).isEmpty();
    }

    /** Dizer só "enviado" esconderia quem não recebeu. */
    @Test
    @DisplayName("um endereço que falha não derruba os outros, e volta na resposta")
    void falhaParcial() {
        when(recipients.emails()).thenReturn(List.of("quebrado@x.com", "rh@proautokimium.com.br"));
        when(reportService.generate(any(), any(), any(), any(), any())).thenReturn(PDF);
        doThrow(new RuntimeException("smtp")).when(emailQueueService)
                .sendNow(eq("quebrado@x.com"), anyString(), anyString(), anyString(), any());

        ReportEmailResultDTO result = service.sendToHr(FROM, TO, STATUS, null, "carla.rh");

        assertThat(result.sentTo()).containsExactly("rh@proautokimium.com.br");
        assertThat(result.failed()).containsExactly("quebrado@x.com");
    }

    @Test
    @DisplayName("se ninguém recebeu, é falha técnica (503), não sucesso vazio")
    void ninguemRecebeu() {
        when(recipients.emails()).thenReturn(List.of("rh@proautokimium.com.br"));
        when(reportService.generate(any(), any(), any(), any(), any())).thenReturn(PDF);
        doThrow(new RuntimeException("smtp")).when(emailQueueService)
                .sendNow(anyString(), anyString(), anyString(), anyString(), any());

        assertThrows(ReportEmailFailedException.class,
                () -> service.sendToHr(FROM, TO, STATUS, null, "carla.rh"));
    }

    @Test
    @DisplayName("PDF acima do limite do e-mail é recusado, e nada sai")
    void grandeDemais() {
        when(recipients.emails()).thenReturn(List.of("rh@proautokimium.com.br"));
        when(reportService.generate(any(), any(), any(), any(), any()))
                .thenReturn(new byte[(int) ReimbursementReportEmailService.MAX_ATTACHMENT_BYTES + 1]);

        ReportTooLargeForEmailException e = assertThrows(ReportTooLargeForEmailException.class,
                () -> service.sendToHr(FROM, TO, STATUS, null, "carla.rh"));
        assertThat(e.getMessage()).contains("16 MB", "limite de 15 MB");
        verify(emailQueueService, never()).sendNow(anyString(), anyString(), anyString(), anyString(), any());
    }

    @Test
    @DisplayName("o limite exato ainda sai")
    void limiteExato() {
        when(recipients.emails()).thenReturn(List.of("rh@proautokimium.com.br"));
        when(reportService.generate(any(), any(), any(), any(), any()))
                .thenReturn(new byte[(int) ReimbursementReportEmailService.MAX_ATTACHMENT_BYTES]);

        service.sendToHr(FROM, TO, STATUS, null, "carla.rh");

        verify(emailQueueService, times(1)).sendNow(anyString(), anyString(), anyString(), anyString(), any());
    }
}
