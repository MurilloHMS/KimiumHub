package com.proautokimium.api.Infrastructure.services.email;

import com.proautokimium.api.Infrastructure.repositories.email.EmailQueueRepository;
import com.proautokimium.api.Infrastructure.services.storage.EmployeeDocumentStorageService;
import com.proautokimium.api.domain.entities.email.EmailQueue;
import com.proautokimium.api.domain.enums.EmailStatus;
import com.proautokimium.api.domain.enums.email.EmailOrigin;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.mail.MailSendException;

import java.time.Clock;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/** A porta única de saída: enfileirar, enviar na hora, remetente da configuração e anexos no disco. */
class EmailQueueServiceTest {

    private static final LocalDateTime AGORA = LocalDateTime.of(2026, 10, 7, 9, 0);

    private final EmailQueueRepository repository = mock(EmailQueueRepository.class);
    private final EmailSenderResolver senders = mock(EmailSenderResolver.class);
    private final EmailDispatcher dispatcher = mock(EmailDispatcher.class);
    private final EmployeeDocumentStorageService storage = mock(EmployeeDocumentStorageService.class);
    private EmailQueueService service;

    @BeforeEach
    void setUp() {
        Clock clock = Clock.fixed(AGORA.atZone(ZoneId.systemDefault()).toInstant(), ZoneId.systemDefault());
        service = new EmailQueueService(repository, senders, dispatcher, storage, clock);
        when(senders.resolve(any())).thenReturn(new EmailSenderResolver.Sender("rh@envios.proautokimium.com.br", "RH Proauto", "rh@proautokimium.com.br"));
        when(repository.save(any())).thenAnswer(c -> c.getArgument(0));
    }

    @Test
    @DisplayName("enfileirar: agendado, com a origem e o remetente da configuração")
    void enfileira() {
        EmailQueue e = service.enqueue(EmailOrigin.DOCUMENT_ALERT, "ana@x.com", "ASO vence", "<p>x</p>");

        assertThat(e.getStatus()).isEqualTo(EmailStatus.SCHEDULED);
        assertThat(e.getOrigin()).isEqualTo(EmailOrigin.DOCUMENT_ALERT);
        assertThat(e.getFromEmail()).isEqualTo("rh@envios.proautokimium.com.br");
        assertThat(e.getFromName()).isEqualTo("RH Proauto");
        assertThat(e.getReplyTo()).isEqualTo("rh@proautokimium.com.br");
        assertThat(e.getCreatedAt()).isEqualTo(AGORA);
        verifyNoInteractions(dispatcher);
    }

    @Test
    @DisplayName("anexo vai para o disco na pasta dos anexos, e a fila guarda onde está")
    void anexoNoDisco() throws Exception {
        when(storage.save(any(), eq(EmailQueueService.ATTACHMENT_FOLDER), eq("relatorio.pdf"))).thenReturn("email-anexos/abc-relatorio.pdf");

        EmailQueue e = service.enqueue(EmailOrigin.REIMBURSEMENT_REPORT, "rh@x.com", "Relatório", "<p>x</p>",
                List.of(new EmailQueueService.OutgoingAttachment("relatorio.pdf", new byte[]{1, 2, 3}, "application/pdf")));

        assertThat(e.getAttachments()).singleElement().satisfies(a -> {
            assertThat(a.getStoragePath()).isEqualTo("email-anexos/abc-relatorio.pdf");
            assertThat(a.getSizeBytes()).isEqualTo(3);
            assertThat(a.getContentType()).isEqualTo("application/pdf");
        });
    }

    @Test
    @DisplayName("enviar na hora: SENT, uma tentativa, e a linha é gravada")
    void enviaNaHora() {
        service.sendNow(EmailOrigin.FIRST_ACCESS, "ana@x.com", "Seu código", "<p>123</p>");

        ArgumentCaptor<EmailQueue> salvo = ArgumentCaptor.forClass(EmailQueue.class);
        verify(repository).save(salvo.capture());
        assertThat(salvo.getValue().getStatus()).isEqualTo(EmailStatus.SENT);
        assertThat(salvo.getValue().getAttempts()).isEqualTo(1);
        assertThat(salvo.getValue().getSentAt()).isEqualTo(AGORA);
        verify(dispatcher).send(salvo.getValue());
    }

    @Test
    @DisplayName("falha no envio na hora: FAILED com o motivo gravado, e a falha sobe")
    void falhaNaHora() {
        doThrow(new MailSendException("falhou", new RuntimeException("550 5.1.1 User unknown"))).when(dispatcher).send(any());

        assertThatThrownBy(() -> service.sendNow(EmailOrigin.FIRST_ACCESS, "x@x.com", "Seu código", "<p>1</p>"))
                .isInstanceOf(MailSendException.class);

        ArgumentCaptor<EmailQueue> salvo = ArgumentCaptor.forClass(EmailQueue.class);
        verify(repository).save(salvo.capture());
        assertThat(salvo.getValue().getStatus()).isEqualTo(EmailStatus.FAILED);
        assertThat(salvo.getValue().getLastError()).contains("550 5.1.1 User unknown");
        assertThat(salvo.getValue().getLastAttemptAt()).isEqualTo(AGORA);
    }

    @Test
    @DisplayName("envio manual: sai do remetente escolhido, não do da configuração")
    void envioManual() {
        EmailQueue e = service.enqueueAs("comercial@envios.proautokimium.com.br", "Comercial", " ", "a@x.com", "Oi", "<p>Oi</p>", List.of());

        assertThat(e.getOrigin()).isEqualTo(EmailOrigin.MANUAL);
        assertThat(e.getFromEmail()).isEqualTo("comercial@envios.proautokimium.com.br");
        assertThat(e.getReplyTo()).isNull();
    }

    @Test
    @DisplayName("e-mail montado pela fábrica (candidaturas) ganha o remetente na hora de entrar na fila")
    void criaComRemetente() {
        EmailQueue montado = EmailQueue.of(EmailOrigin.RECRUITMENT, "c@x.com", "Recebemos", "<p>x</p>", AGORA);

        EmailQueue e = service.create(montado);

        assertThat(e.getFromEmail()).isEqualTo("rh@envios.proautokimium.com.br");
        assertThat(e.getStatus()).isEqualTo(EmailStatus.SCHEDULED);
        verify(senders).resolve(EmailOrigin.RECRUITMENT);
    }

    @Test
    @DisplayName("o motivo guardado traz a cadeia de exceções, onde está o código do servidor")
    void descreveCadeia() {
        String d = EmailQueueService.describe(new RuntimeException("embrulho", new IllegalStateException("552 Mailbox full")));
        assertThat(d).isEqualTo("RuntimeException: embrulho ← IllegalStateException: 552 Mailbox full");
    }
}
