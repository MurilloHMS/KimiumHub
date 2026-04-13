package com.proautokimium.api.Application.DTOs.processoSeletivo.perguntas;

import com.proautokimium.api.domain.enums.processoSeletivo.TipoPergunta;

public record CreatePerguntaDTO(
        String enunciado,
        TipoPergunta tipo,
        boolean obrigatoria,
        short ordem
) {
}
