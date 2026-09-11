package com.proautokimium.api.Application.DTOs.processoSeletivo.talentBank;

import java.time.LocalDateTime;
import java.util.List;

/**
 * O que a própria pessoa vê ao abrir o link do e-mail.
 *
 * <p>Duas ausências deliberadas:
 *
 * <p><b>Não devolve o {@code pathCurriculo}.</b> O nome do arquivo é
 * {@code <id-do-candidato>.pdf}: devolvê-lo entrega o id interno e o nome exato
 * que o endpoint interno de download aceita. {@code temCurriculo} mais a
 * extensão dizem tudo que a tela precisa.
 *
 * <p><b>As candidaturas vêm sem etapa e sem status.</b> O estado do funil é
 * anotação interna de trabalho; mostrar "TRIAGEM" ou "REPROVADO" ao candidato
 * cria expectativa e pergunta que o RH vai ter que responder — e a reprovação
 * já tem e-mail próprio.
 */
public record TalentBankEntryDTO(
        String nome,
        String email,
        String telefone,
        String urlLinkedin,
        String areaInteresse,
        boolean temCurriculo,
        String extensaoCurriculo,
        LocalDateTime criadoEm,
        LocalDateTime atualizadoEm,
        LocalDateTime consentimentoEm,
        LocalDateTime expiraEm,
        List<CandidaturaResumoDTO> candidaturas
) {
    public record CandidaturaResumoDTO(String vagaTitulo, LocalDateTime criadoEm) { }
}
