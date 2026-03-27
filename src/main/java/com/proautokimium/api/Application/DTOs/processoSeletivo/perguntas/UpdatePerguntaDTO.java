package com.proautokimium.api.Application.DTOs.processoSeletivo.perguntas;

import com.proautokimium.api.domain.enums.processoSeletivo.TipoPergunta;

import java.util.UUID;

public record UpdatePerguntaDTO(
        UUID id,
        String enunciado,
        TipoPergunta tipo,
        boolean obrigatoria,
        short ordem
) {
}