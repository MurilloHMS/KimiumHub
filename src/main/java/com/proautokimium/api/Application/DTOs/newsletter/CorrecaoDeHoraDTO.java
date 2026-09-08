package com.proautokimium.api.Application.DTOs.newsletter;

import jakarta.validation.constraints.NotNull;

import java.time.LocalTime;

/**
 * A hora digitada na revisão para uma OS que não deu para ler.
 *
 * `LocalTime` e não texto: o campo da tela é um `input type="time"`, que já
 * entrega `16:03`. Aceitar texto livre aqui recriaria, do lado de dentro, o
 * mesmo problema que esta tela existe para resolver.
 */
public record CorrecaoDeHoraDTO(
        @NotNull LocalTime horaInicio,
        @NotNull LocalTime horaFim
) {
}
