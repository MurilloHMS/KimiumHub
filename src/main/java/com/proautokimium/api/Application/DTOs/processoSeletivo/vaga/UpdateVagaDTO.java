package com.proautokimium.api.Application.DTOs.processoSeletivo.vaga;

import java.time.LocalDateTime;
import java.util.UUID;

public record UpdateVagaDTO(
        UUID id,
        String titulo,
        String descricao,
        String requisitos,
        String beneficios,
        String area,
        LocalDateTime dataAbertura,
        LocalDateTime dataEncerramento
) {}