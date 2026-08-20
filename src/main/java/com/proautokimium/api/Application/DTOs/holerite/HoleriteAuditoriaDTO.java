package com.proautokimium.api.Application.DTOs.holerite;

import com.proautokimium.api.domain.enums.HoleriteTipo;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Uma linha da auditoria do RH.
 *
 * `temUsuario` responde "essa pessoa consegue ser avisada?". Sem ele a tela
 * mostraria "nunca abriu" para quem nunca foi notificado, e o RH cobraria a
 * pessoa errada.
 */
public record HoleriteAuditoriaDTO(
        UUID id,
        UUID employeeId,
        String employeeNome,
        String codParceiro,
        LocalDate competencia,
        HoleriteTipo tipo,
        String originalFilename,
        LocalDateTime createdAt,
        LocalDateTime openedAt,
        LocalDateTime confirmedAt,
        LocalDateTime canceledAt,
        String canceledBy,
        String cancelReason,
        LocalDateTime replacedAt,
        boolean temUsuario) { }