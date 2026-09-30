package com.proautokimium.api.domain.enums.sales;

/**
 * Onde o checklist está.
 *
 * <pre>
 * SUBMITTED ──aprovar──▶ APPROVED
 *     │                     │
 *     ├──devolver──▶ RETURNED ──reenviar──▶ SUBMITTED
 *     │                     │
 *     └──────pedir alteração┴──▶ CHANGE_REQUESTED ──liberar──▶ REOPENED ──reenviar──▶ SUBMITTED
 *                                        └──negar──▶ volta para onde estava
 * </pre>
 *
 * Só RETURNED e REOPENED aceitam edição: depois de enviado, mudar qualquer coisa
 * passa pela Controladoria (pedido dele em 2026-09-29).
 */
public enum ChecklistStatus {
    SUBMITTED,
    APPROVED,
    RETURNED,
    CHANGE_REQUESTED,
    REOPENED;

    public boolean editable() {
        return this == RETURNED || this == REOPENED;
    }
}
