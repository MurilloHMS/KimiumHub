package com.proautokimium.api.Application.DTOs.processoSeletivo.talentBank;

import java.util.UUID;
import java.time.LocalDateTime;

/**
 * Uma linha da aba interna do banco de talentos.
 *
 * <p>DTO próprio, e não o {@code ResponseCandidatoDTO} que já existe: aquele
 * <b>não tem id</b> — a tela não conseguiria linkar em nada — e expõe o
 * {@code pathCurriculo}.
 *
 * @param espontaneo sem nenhuma candidatura. É um <b>filtro</b>, não a
 *                   definição de estar no banco: quem se candidatou em março e
 *                   foi reprovado continua aqui
 * @param consentimentoEm {@code null} desenha "sem consentimento registrado" na
 *                        tela, e é o estado das linhas anteriores a 2026-09-11
 */
public record TalentBankSummaryDTO(
        UUID id,
        String nome,
        String email,
        String telefone,
        String urlLinkedin,
        String areaInteresse,
        boolean temCurriculo,
        boolean espontaneo,
        int quantidadeDeCandidaturas,
        LocalDateTime criadoEm,
        LocalDateTime atualizadoEm,
        LocalDateTime consentimentoEm,
        LocalDateTime expiraEm
) { }
