package com.proautokimium.api.Application.DTOs.newsletter;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;

import java.util.List;

/**
 * Vários de uma vez.
 *
 * São 31 clientes sem e-mail em junho, e um por requisição faria a tela
 * disparar 31 chamadas para um trabalho que é um só.
 */
public record PreencherEmailsDTO(
        @NotEmpty @Valid List<EmailPreenchidoDTO> emails
) {
}
