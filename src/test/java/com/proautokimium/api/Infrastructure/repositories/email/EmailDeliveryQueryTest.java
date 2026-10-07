package com.proautokimium.api.Infrastructure.repositories.email;

import com.proautokimium.api.domain.entities.email.EmailQueue;
import com.proautokimium.api.domain.enums.EmailStatus;
import com.proautokimium.api.domain.enums.email.EmailOrigin;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * As consultas do rastreio de entrega rodam num banco de verdade: JPQL errada
 * só aparece ao executar. Sem a transação do teste, como o agendador.
 */
@DataJpaTest
@ActiveProfiles("test")
@Transactional(propagation = Propagation.NOT_SUPPORTED)
class EmailDeliveryQueryTest {

    private static final LocalDateTime AGORA = LocalDateTime.of(2026, 10, 7, 15, 0);

    @Autowired EmailQueueRepository repository;

    @AfterEach
    void tearDown() {
        repository.deleteAll();
    }

    private EmailQueue enviado(LocalDateTime em) {
        EmailQueue e = EmailQueue.of(EmailOrigin.CHECKLIST, "a@x.com", "s", "<p>x</p>", em);
        e.assignSender("noreply@envios.proautokimium.com.br", "Proauto Kimium", null);
        e.markSent(em);
        return repository.save(e);
    }

    @Test
    @DisplayName("esperando entrega: só enviados rastreados, sem confirmação, dentro da janela")
    void esperando() {
        EmailQueue recente = enviado(AGORA.minusHours(1));
        enviado(AGORA.minusDays(4)); // fora da janela
        EmailQueue jaEntregue = enviado(AGORA.minusHours(2));
        repository.markDelivered(jaEntregue.getId(), AGORA.minusHours(2));
        EmailQueue antigo = new EmailQueue("b@x.com", "noreply@x.com", "s", "b"); // sem tracking_id
        antigo.markSent(AGORA.minusHours(1));
        repository.save(antigo);

        List<EmailQueueRepository.AwaitingDelivery> r =
                repository.findAwaitingDelivery(EmailStatus.SENT, AGORA.minus(EmailQueue.DELIVERY_WINDOW));

        assertThat(r).extracting(EmailQueueRepository.AwaitingDelivery::getId).containsExactly(recente.getId());
        assertThat(r.getFirst().getTrackingId()).isEqualTo(recente.getTrackingId());
    }

    @Test
    @DisplayName("marcar entregue vale uma vez: a segunda passada não reescreve a hora")
    void marcaUmaVez() {
        EmailQueue e = enviado(AGORA.minusHours(1));

        assertThat(repository.markDelivered(e.getId(), AGORA.minusMinutes(59))).isEqualTo(1);
        assertThat(repository.markDelivered(e.getId(), AGORA)).isZero();
        assertThat(repository.findById(e.getId()).orElseThrow().getDeliveredAt()).isEqualTo(AGORA.minusMinutes(59));
    }

    @Test
    @DisplayName("contagem da entrega: só os rastreados, com entregues e devolvidos")
    void contagem() {
        EmailQueue a = enviado(AGORA.minusHours(1));
        EmailQueue b = enviado(AGORA.minusHours(1));
        enviado(AGORA.minusHours(1));
        repository.markDelivered(a.getId(), AGORA);
        repository.markBounced(b.getId(), AGORA, "550");
        EmailQueue antigo = new EmailQueue("b@x.com", "noreply@x.com", "s", "b");
        antigo.markSent(AGORA.minusHours(1));
        repository.save(antigo);

        EmailQueueRepository.DeliveryCount c =
                repository.countDeliverySince(List.of(EmailStatus.SENT, EmailStatus.FAILED), AGORA.minusDays(1));

        assertThat(c.getDone()).isEqualTo(3);
        assertThat(c.getDelivered()).isEqualTo(1);
        assertThat(c.getBounced()).isEqualTo(1);
        assertThat(repository.countAwaitingDeliverySince(EmailStatus.SENT, AGORA.minusDays(1))).isEqualTo(1);
    }
}
