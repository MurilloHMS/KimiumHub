package com.proautokimium.api.Infrastructure.services.email;

import com.proautokimium.api.Infrastructure.repositories.email.EmailQueueRepository;
import com.proautokimium.api.Infrastructure.services.email.smtp.SmtpService;
import com.proautokimium.api.domain.entities.email.EmailQueue;
import com.proautokimium.api.domain.enums.EmailStatus;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

class EmailQueueServiceTest {

    private final EmailQueueRepository repository = mock(EmailQueueRepository.class);
    private final SmtpService smtpService = mock(SmtpService.class);
    private final EmailQueueService service = new EmailQueueService(repository, smtpService);

    @Test
    @DisplayName("Deve enviar o email imediatamente e registrar como SENT")
    void shouldSendImmediatelyAndPersistAsSent() {
        service.sendNow("funcionario@teste.com", "noreply@envios.proautokimium.com.br", "Token de primeiro acesso", "Seu token: ABC123");

        verify(smtpService).sendEmail(any(EmailQueue.class));

        ArgumentCaptor<EmailQueue> captor = ArgumentCaptor.forClass(EmailQueue.class);
        verify(repository).save(captor.capture());

        EmailQueue saved = captor.getValue();
        assertThat(saved.getStatus()).isEqualTo(EmailStatus.SENT);
        assertThat(saved.getToEmail()).isEqualTo("funcionario@teste.com");
    }

    @Test
    @DisplayName("Falha no SMTP deve registrar como FAILED e propagar a exceção")
    void shouldPersistAsFailedAndPropagateExceptionWhenSmtpFails() {
        doThrow(new RuntimeException("SMTP fora do ar")).when(smtpService).sendEmail(any(EmailQueue.class));

        assertThatThrownBy(() ->
                service.sendNow("funcionario@teste.com", "noreply@envios.proautokimium.com.br", "Token de primeiro acesso", "Seu token: ABC123")
        ).isInstanceOf(RuntimeException.class);

        ArgumentCaptor<EmailQueue> captor = ArgumentCaptor.forClass(EmailQueue.class);
        verify(repository).save(captor.capture());
        assertThat(captor.getValue().getStatus()).isEqualTo(EmailStatus.FAILED);
    }
}
