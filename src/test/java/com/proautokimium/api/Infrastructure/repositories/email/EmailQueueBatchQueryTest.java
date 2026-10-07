package com.proautokimium.api.Infrastructure.repositories.email;

import com.proautokimium.api.domain.entities.email.EmailQueue;
import com.proautokimium.api.domain.enums.EmailStatus;
import com.proautokimium.api.domain.enums.email.EmailOrigin;
import org.hibernate.Hibernate;
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
 * **O lote do agendador põe o LIMIT no SQL.**
 *
 * <p>A opção {@code fail_on_pagination_over_collection_fetch} transforma o
 * "corto na memória" do Hibernate, que em produção é só um aviso no log, em
 * erro. Com o {@code findTop15} + {@code @EntityGraph} de antes, este teste
 * quebra.
 *
 * <p>Sem a transação do {@code @DataJpaTest}: o agendador lê fora de
 * transação, e é aí que um anexo preguiçoso daria LazyInitializationException.
 */
@DataJpaTest(properties = "spring.jpa.properties.hibernate.query.fail_on_pagination_over_collection_fetch=true")
@ActiveProfiles("test")
@Transactional(propagation = Propagation.NOT_SUPPORTED)
class EmailQueueBatchQueryTest {

    private static final LocalDateTime INICIO = LocalDateTime.of(2026, 10, 7, 9, 0);

    @Autowired EmailQueueRepository repository;

    @AfterEach
    void tearDown() {
        repository.deleteAll();
    }

    private EmailQueue email(int minuto, EmailStatus status) {
        EmailQueue e = EmailQueue.of(EmailOrigin.NEWSLETTER, "c" + minuto + "@x.com", "Resumo", "<p>x</p>", INICIO.plusMinutes(minuto));
        e.assignSender("newsletter@envios.proautokimium.com.br", "Proauto Kimium", null);
        e.setStatus(status);
        e.addAttachment("resumo.pdf", "application/pdf", "email-anexos/" + minuto + ".pdf", 10, INICIO);
        return e;
    }

    @Test
    @DisplayName("devolve os 15 mais antigos da fila, em ordem, com os anexos já carregados")
    void quinzeMaisAntigos() {
        for (int m = 20; m >= 0; m--) {
            repository.save(email(m, m % 2 == 0 ? EmailStatus.PENDING : EmailStatus.SCHEDULED));
        }
        repository.save(email(-1, EmailStatus.FAILED));

        List<EmailQueue> lote = repository.nextBatch(List.of(EmailStatus.PENDING, EmailStatus.SCHEDULED));

        assertThat(lote).hasSize(EmailQueueRepository.BATCH_SIZE);
        assertThat(lote).extracting(EmailQueue::getCreatedAt).isSorted()
                .first().isEqualTo(INICIO);
        assertThat(lote).allSatisfy(e -> {
            assertThat(Hibernate.isInitialized(e.getAttachments())).isTrue();
            assertThat(e.getAttachments()).hasSize(1);
        });
    }

    @Test
    @DisplayName("fila vazia: lista vazia, sem a segunda consulta")
    void filaVazia() {
        assertThat(repository.nextBatch(List.of(EmailStatus.PENDING))).isEmpty();
    }
}
