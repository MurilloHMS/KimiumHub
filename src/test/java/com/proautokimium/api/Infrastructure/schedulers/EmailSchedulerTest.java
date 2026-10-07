package com.proautokimium.api.Infrastructure.schedulers;

import com.proautokimium.api.Infrastructure.repositories.email.EmailQueueRepository;
import com.proautokimium.api.Infrastructure.services.email.EmailDispatcher;
import com.proautokimium.api.domain.entities.email.EmailQueue;
import com.proautokimium.api.domain.enums.EmailStatus;
import com.proautokimium.api.domain.enums.email.EmailOrigin;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.mail.MailSendException;

import java.time.Clock;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/** O agendador grava o motivo de cada falha; antes, a exceção sumia. */
class EmailSchedulerTest {

    @Test
    @DisplayName("um falha e o outro sai: o que falhou guarda o motivo e volta para a fila")
    void gravaMotivo() {
        LocalDateTime agora = LocalDateTime.of(2026, 10, 7, 9, 0);
        EmailQueueRepository repo = mock(EmailQueueRepository.class);
        EmailDispatcher dispatcher = mock(EmailDispatcher.class);
        EmailQueue falha = EmailQueue.of(EmailOrigin.NEWSLETTER, "x@x.com", "a", "b", agora);
        EmailQueue ok = EmailQueue.of(EmailOrigin.NEWSLETTER, "y@x.com", "a", "b", agora);
        // A Entity base compara só o id: sem id, as duas linhas seriam "iguais" para o Mockito.
        falha.id = java.util.UUID.randomUUID();
        ok.id = java.util.UUID.randomUUID();
        when(repo.findTop15ByStatusInOrderByCreatedAtAsc(any())).thenReturn(List.of(falha, ok));
        doThrow(new MailSendException("falhou", new RuntimeException("552 Mailbox full"))).when(dispatcher).send(falha);

        new EmailScheduler(repo, dispatcher, Clock.fixed(agora.atZone(ZoneId.systemDefault()).toInstant(), ZoneId.systemDefault()))
                .processQueue();

        assertThat(falha.getStatus()).isEqualTo(EmailStatus.PENDING);
        assertThat(falha.getAttempts()).isEqualTo(1);
        assertThat(falha.getLastError()).contains("552 Mailbox full");
        assertThat(ok.getStatus()).isEqualTo(EmailStatus.SENT);
        verify(repo).save(falha);
        verify(repo).save(ok);
    }
}
