package com.proautokimium.api.Application.DTOs.processoSeletivo.candidaturas;

import java.util.UUID;

/**
 * Candidatura a uma vaga, pelo formulário público.
 *
 * @param consentimento autorização para <b>permanecer no banco de talentos</b>
 *                      depois deste processo. <b>Opcional aqui</b>, ao
 *                      contrário da inscrição espontânea: candidatar-se a uma
 *                      vaga específica é finalidade legítima por si só, e ficar
 *                      disponível para vagas futuras é outra coisa. Não marcado,
 *                      a candidatura é criada do mesmo jeito e
 *                      {@code consentimento_em} continua nulo — que é
 *                      exatamente o estado das linhas anteriores a 2026-09-11.
 */
public record CreateCandidaturaDTO(
        UUID vagaID,
        String nome,
        String email,
        String telefone,
        String urlLinkedin,
        boolean consentimento
) {
}
