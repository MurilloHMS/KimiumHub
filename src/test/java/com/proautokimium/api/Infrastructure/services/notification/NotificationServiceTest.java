package com.proautokimium.api.Infrastructure.services.notification;

import com.proautokimium.api.Infrastructure.repositories.NotificationRepository;
import com.proautokimium.api.Infrastructure.services.push.WebPushService;
import com.proautokimium.api.domain.entities.Notification;
import com.proautokimium.api.domain.enums.NotificationType;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * **O push só sai depois que a notificação existe de verdade no banco.**
 *
 * Em 2026-09-28 o RH mandou dois avisos: o celular recebeu o push, e nada
 * apareceu na lista — a transação desfez tudo depois que o push já tinha
 * saído. Dentro de um {@code @Transactional}, o {@code save} só grava no
 * commit; entregar antes disso é anunciar o que pode não existir.
 *
 * A transação aqui é simulada pelo {@link TransactionSynchronizationManager},
 * que é exatamente o mecanismo que o Spring usa no commit.
 */
class NotificationServiceTest {

    private final NotificationRepository repository = mock(NotificationRepository.class);
    private final SimpMessagingTemplate messaging = mock(SimpMessagingTemplate.class);
    private final WebPushService webPush = mock(WebPushService.class);
    private final NotificationService service = new NotificationService(repository, messaging, webPush);

    @BeforeEach
    void setUp() {
        when(repository.save(any(Notification.class))).thenAnswer(inv -> inv.getArgument(0));
    }

    @AfterEach
    void limpar() {
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.clearSynchronization();
        }
    }

    private void notificar() {
        service.notify("ana", NotificationType.PERSONALIZADA, "Reunião", "Amanhã às 9h", "/documentos");
    }

    @Test
    @DisplayName("dentro de uma transação, nada é entregue antes do commit")
    void nadaAntesDoCommit() {
        TransactionSynchronizationManager.initSynchronization();

        notificar();

        verify(webPush, never()).sendToUser(anyString(), anyString(), anyString(), any());
        verify(messaging, never()).convertAndSendToUser(anyString(), anyString(), any(Object.class));

        TransactionSynchronizationManager.getSynchronizations().forEach(TransactionSynchronization::afterCommit);

        verify(webPush).sendToUser("ana", "Reunião", "Amanhã às 9h", "/documentos");
        verify(messaging).convertAndSendToUser(eq("ana"), eq("/queue/notifications"), any(Object.class));
    }

    @Test
    @DisplayName("se a transação desfaz, ninguém recebe push")
    void rollbackNaoEntrega() {
        TransactionSynchronizationManager.initSynchronization();

        notificar();
        TransactionSynchronizationManager.getSynchronizations()
                .forEach(s -> s.afterCompletion(TransactionSynchronization.STATUS_ROLLED_BACK));

        verify(webPush, never()).sendToUser(anyString(), anyString(), anyString(), any());
    }

    @Test
    @DisplayName("fora de transação, entrega na hora")
    void semTransacaoEntregaNaHora() {
        notificar();
        verify(webPush).sendToUser("ana", "Reunião", "Amanhã às 9h", "/documentos");
    }

    /**
     * O aviso do mural aceita 4000 caracteres e manda o texto inteiro como
     * mensagem; a coluna tem 500. Passar disso derrubava a transação no commit.
     */
    @Test
    @DisplayName("título, mensagem e link são cortados no tamanho das colunas, com reticências")
    void cortaNoTamanhoDaColuna() {
        service.notify("ana", NotificationType.GERAL, "T".repeat(250), "M".repeat(600), "/" + "l".repeat(400));

        ArgumentCaptor<Notification> salva = ArgumentCaptor.forClass(Notification.class);
        verify(repository).save(salva.capture());
        assertThat(salva.getValue().getTitle()).hasSize(200).endsWith("…");
        assertThat(salva.getValue().getMessage()).hasSize(500).endsWith("…");
        assertThat(salva.getValue().getLink()).as("link cortado não abre: vai sem link").isNull();
    }
}
