package com.proautokimium.api.domain.enums;

public enum ReconciliationOutcome {
    CREATED,
    UPDATED,
    DEACTIVATED,
    /** O ERP mudou entre a prévia e o aplicar: a assinatura não bate. */
    SKIPPED_CHANGED_IN_ERP,
    /** O código sumiu do ERP entre a prévia e o aplicar. */
    SKIPPED_GONE_FROM_ERP,
    /** A linha tem impedimento; nada foi gravado. */
    REFUSED_IMPEDIMENT,
    /** Já estava igual — nada a fazer. */
    SKIPPED_ALREADY_EQUAL,
    /** Falhou ao gravar. As outras linhas continuaram. */
    REFUSED_ERROR
}
