package com.proautokimium.api.Application.DTOs.processoSeletivo.vaga;

import java.time.LocalDateTime;

public record CreateVagaDTO(
        String titulo,
        String descricao,
        String requisitos,
        String beneficios,
        String area,
        LocalDateTime dataAbertura,
        LocalDateTime dataEncerramento
) {
}
