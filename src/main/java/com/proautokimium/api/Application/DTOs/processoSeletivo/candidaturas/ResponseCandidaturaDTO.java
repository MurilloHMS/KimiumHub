package com.proautokimium.api.Application.DTOs.processoSeletivo.candidaturas;

import com.proautokimium.api.domain.entities.processoSeletivo.Candidatura;
import com.proautokimium.api.domain.enums.processoSeletivo.Etapa;
import com.proautokimium.api.domain.enums.processoSeletivo.StatusCandidatura;

import java.time.LocalDateTime;
import java.util.UUID;

public record ResponseCandidaturaDTO(
        UUID id,
        String candidatoNome,
        String candidatoEmail,
        String candidatoTelefone,
        String candidatoLinkedin,
        String candidatoCurriculo,
        String vagaTitulo,
        Etapa etapaAtual,
        StatusCandidatura status,
        LocalDateTime criadoEm,
        LocalDateTime atualizadoEm
) {}