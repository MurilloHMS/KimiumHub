package com.proautokimium.api.Infrastructure.services.email;

import com.proautokimium.api.Infrastructure.repositories.email.EmailQueueRepository;
import com.proautokimium.api.Infrastructure.services.storage.EmployeeDocumentStorageService;
import com.proautokimium.api.domain.entities.email.EmailQueue;
import com.proautokimium.api.domain.enums.EmailStatus;
import com.proautokimium.api.domain.enums.email.EmailOrigin;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.mail.MailSendException;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.Clock;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * **A linha do {@code sendNow} sobrevive ao rollback de quem chamou.**
 *
 * <p>Quem chama o {@code sendNow} (redefinição de senha, primeiro acesso) está
 * dentro de uma transação, e a falha do envio sobe de propósito. Sem transação
 * própria, o rollback levava junto a linha FAILED, e a tela da fila nunca via
 * justamente as falhas que mais importam. Achado na prova ponta a ponta de
 * 2026-10-07: o SMTP fora do ar devolveu 500 e a fila ficou vazia.
 *
 * <p>Banco de verdade (H2) porque um mock do repositório não tem rollback.
 */
@DataJpaTest
@ActiveProfiles("test")
@Transactional(propagation = Propagation.NOT_SUPPORTED) // a transação de fora é a do teste, não a do @DataJpaTest
class EmailSendNowRollbackTest {

    @Autowired EmailQueueRepository repository;
    @Autowired PlatformTransactionManager transactionManager;

    private final EmailDispatcher dispatcher = mock(EmailDispatcher.class);
    private EmailQueueService service;

    @BeforeEach
    void setUp() {
        EmailSenderResolver senders = mock(EmailSenderResolver.class);
        when(senders.resolve(any())).thenReturn(new EmailSenderResolver.Sender("noreply@envios.proautokimium.com.br", "Proauto Kimium", null));
        service = new EmailQueueService(repository, senders, dispatcher, mock(EmployeeDocumentStorageService.class),
                Clock.systemDefaultZone(), transactionManager);
    }

    @AfterEach
    void tearDown() {
        repository.deleteAll();
    }

    @Test
    @DisplayName("falha na hora dentro de uma transação que volta: a linha FAILED fica gravada")
    void falhaSobreviveAoRollback() {
        doThrow(new MailSendException("Couldn't connect to host")).when(dispatcher).send(any());
        TransactionTemplate quemChamou = new TransactionTemplate(transactionManager);

        assertThatThrownBy(() -> quemChamou.executeWithoutResult(s ->
                service.sendNow(EmailOrigin.PASSWORD_RESET, "ana@x.com", "Redefinição", "<p>1</p>")))
                .isInstanceOf(MailSendException.class);

        assertThat(repository.findAll()).singleElement().satisfies(e -> {
            assertThat(e.getStatus()).isEqualTo(EmailStatus.FAILED);
            assertThat(e.getOrigin()).isEqualTo(EmailOrigin.PASSWORD_RESET);
            assertThat(e.getLastError()).contains("Couldn't connect to host");
        });
    }

    @Test
    @DisplayName("enviado e depois quem chamou volta: o registro do envio fica, porque o e-mail saiu")
    void enviadoFicaMesmoComRollback() {
        TransactionTemplate quemChamou = new TransactionTemplate(transactionManager);

        quemChamou.executeWithoutResult(s -> {
            service.sendNow(EmailOrigin.FIRST_ACCESS, "ana@x.com", "Seu código", "<p>1</p>");
            s.setRollbackOnly();
        });

        assertThat(repository.findAll()).extracting(EmailQueue::getStatus).containsExactly(EmailStatus.SENT);
    }
}
