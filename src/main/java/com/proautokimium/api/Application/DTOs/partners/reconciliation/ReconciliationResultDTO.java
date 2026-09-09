package com.proautokimium.api.Application.DTOs.partners.reconciliation;

import java.util.List;

/**
 * O que o aplicar fez.
 *
 * <p>{@code lines} traz <b>só as que não deram no esperado</b>. Listar as 47 que
 * funcionaram afogaria as 3 que não — e são essas que alguém precisa ver.
 */
public record ReconciliationResultDTO(
        int created,
        int updated,
        int deactivated,
        int skipped,
        List<ReconciliationOutcomeDTO> lines
) { }
