package com.proautokimium.api.Application.DTOs.processoSeletivo.candidato;

public record CreateCandidatoDTO(
        String nome,
        String email,
        String telefone,
        String urlLinkedin,
        String pathCurriculo
) {
}
