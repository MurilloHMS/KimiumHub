package com.proautokimium.api.domain.enums.email;

import lombok.Getter;

import java.util.Locale;

/**
 * O motivo de uma falha de envio, em grupos que a pessoa entende. Vem do texto
 * da exceção do SMTP: o servidor responde com código (550, 535…) e o Java com
 * o nome da exceção. O texto cru continua guardado em {@code last_error}.
 */
@Getter
public enum EmailFailureKind {
    TIMEOUT("Tempo esgotado no servidor de e-mail"),
    MAILBOX_NOT_FOUND("Caixa postal não existe"),
    AUTH("Login no servidor recusado"),
    INVALID_ADDRESS("Endereço de e-mail inválido"),
    MAILBOX_FULL("Caixa do destinatário cheia"),
    OTHER("Outro erro");

    private final String label;

    EmailFailureKind(String label) {
        this.label = label;
    }

    /** null quando não houve erro. A ordem importa: o código do SMTP vence a palavra solta. */
    public static EmailFailureKind classify(String error) {
        if (error == null || error.isBlank()) return null;
        String e = error.toLowerCase(Locale.ROOT);
        if (e.contains("550") || e.contains("5.1.1") || e.contains("user unknown") || e.contains("no such user")
                || e.contains("recipient address rejected")) return MAILBOX_NOT_FOUND;
        if (e.contains("552") || e.contains("5.2.2") || e.contains("mailbox full") || e.contains("quota")) return MAILBOX_FULL;
        if (e.contains("535") || e.contains("authentication")) return AUTH;
        if (e.contains("addressexception") || e.contains("invalid address") || e.contains("missing final")
                || e.contains("illegal address")) return INVALID_ADDRESS;
        if (e.contains("timeout") || e.contains("timed out") || e.contains("couldn't connect")
                || e.contains("connection refused") || e.contains("mailconnectexception")) return TIMEOUT;
        return OTHER;
    }
}
