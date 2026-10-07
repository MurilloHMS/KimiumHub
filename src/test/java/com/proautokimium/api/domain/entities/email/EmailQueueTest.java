package com.proautokimium.api.domain.entities.email;

import com.proautokimium.api.domain.enums.EmailStatus;
import com.proautokimium.api.domain.enums.email.EmailFailureKind;
import com.proautokimium.api.domain.enums.email.EmailOrigin;
import com.proautokimium.api.domain.exceptions.humanResources.InvalidStatusTransitionException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

/** As regras da linha da fila: tentativas, motivo da falha e reenvio. */
class EmailQueueTest {

    private static final LocalDateTime AGORA = LocalDateTime.of(2026, 10, 7, 9, 0);

    private static EmailQueue email() {
        EmailQueue e = EmailQueue.of(EmailOrigin.NEWSLETTER, "a@x.com", "Assunto", "<p>x</p>", AGORA);
        e.markSchedule();
        return e;
    }

    @Test
    @DisplayName("falha volta para a fila até a 5ª tentativa; na 5ª fica FAILED, com o último motivo")
    void cincoTentativas() {
        EmailQueue e = email();
        for (int i = 1; i <= 4; i++) {
            e.recordFailure("timeout " + i, AGORA.plusMinutes(i));
            assertThat(e.getStatus()).as("tentativa %d", i).isEqualTo(EmailStatus.PENDING);
        }
        e.recordFailure("timeout 5", AGORA.plusMinutes(5));

        assertThat(e.getStatus()).isEqualTo(EmailStatus.FAILED);
        assertThat(e.getAttempts()).isEqualTo(5);
        assertThat(e.getLastError()).isEqualTo("timeout 5");
        assertThat(e.getLastAttemptAt()).isEqualTo(AGORA.plusMinutes(5));
    }

    @Test
    @DisplayName("sucesso depois de falhar: SENT, conta a tentativa e limpa o erro")
    void sucessoDepoisDeFalhar() {
        EmailQueue e = email();
        e.recordFailure("timeout", AGORA);
        e.markSent(AGORA.plusMinutes(1));

        assertThat(e.getStatus()).isEqualTo(EmailStatus.SENT);
        assertThat(e.getAttempts()).isEqualTo(2);
        assertThat(e.getLastError()).isNull();
        assertThat(e.getSentAt()).isEqualTo(AGORA.plusMinutes(1));
    }

    @Test
    @DisplayName("envio na hora que falha: FAILED já na primeira, sem voltar para a fila")
    void falhaImediata() {
        EmailQueue e = email();
        e.recordImmediateFailure("550", AGORA);
        assertThat(e.getStatus()).isEqualTo(EmailStatus.FAILED);
        assertThat(e.getAttempts()).isEqualTo(1);
    }

    @Test
    @DisplayName("reenviar: só o que falhou, com tentativas zeradas e erro limpo")
    void reenviar() {
        EmailQueue e = email();
        assertThrows(InvalidStatusTransitionException.class, e::requeue);

        e.recordImmediateFailure("550", AGORA);
        e.requeue();

        assertThat(e.getStatus()).isEqualTo(EmailStatus.PENDING);
        assertThat(e.getAttempts()).isZero();
        assertThat(e.getLastError()).isNull();
    }

    @Test
    @DisplayName("erro muito longo é cortado: a pilha inteira não cabe e o motivo está no começo")
    void erroCortado() {
        EmailQueue e = email();
        e.recordFailure("x".repeat(5000), AGORA);
        assertThat(e.getLastError()).hasSize(EmailQueue.MAX_ERROR_LENGTH);
    }

    @Test
    @DisplayName("os motivos: código do servidor e nome da exceção viram um grupo que a pessoa entende")
    void classificaMotivos() {
        assertThat(EmailFailureKind.classify(null)).isNull();
        assertThat(EmailFailureKind.classify("MailSendException ← SMTPAddressFailedException: 550 5.1.1 User unknown"))
                .isEqualTo(EmailFailureKind.MAILBOX_NOT_FOUND);
        assertThat(EmailFailureKind.classify("552 5.2.2 Mailbox full")).isEqualTo(EmailFailureKind.MAILBOX_FULL);
        assertThat(EmailFailureKind.classify("AuthenticationFailedException: 535 5.7.8")).isEqualTo(EmailFailureKind.AUTH);
        assertThat(EmailFailureKind.classify("AddressException: Missing final '@domain'")).isEqualTo(EmailFailureKind.INVALID_ADDRESS);
        assertThat(EmailFailureKind.classify("MailConnectException: Couldn't connect to host, port: smtp, 587; timeout 5000"))
                .isEqualTo(EmailFailureKind.TIMEOUT);
        assertThat(EmailFailureKind.classify("algo inesperado")).isEqualTo(EmailFailureKind.OTHER);
    }
}
