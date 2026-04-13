package com.proautokimium.api.Application.DTOs.processoSeletivo.candidato;

import java.time.LocalDateTime;

public record ResponseCandidatoDTO(
        String nome,
        String email,
        String telefone,
        String urlLinkedin,
        String pathCurriculo,
        LocalDateTime criadoEm
) {
}
