package com.proautokimium.api.Application.DTOs.processoSeletivo.candidaturas;

import java.util.UUID;

public record CreateCandidaturaDTO(
        UUID vagaID,
        String nome,
        String email,
        String telefone,
        String urlLinkedin
) {
}
