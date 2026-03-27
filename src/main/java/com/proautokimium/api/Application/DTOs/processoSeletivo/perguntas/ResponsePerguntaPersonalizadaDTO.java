package com.proautokimium.api.Application.DTOs.processoSeletivo.perguntas;

import com.proautokimium.api.domain.enums.processoSeletivo.TipoPergunta;

import java.util.UUID;

public record ResponsePerguntaPersonalizadaDTO(
        UUID id,
        String enunciado,
        TipoPergunta tipo,
        Boolean obrigatoria,
        short ordem
) {
}
