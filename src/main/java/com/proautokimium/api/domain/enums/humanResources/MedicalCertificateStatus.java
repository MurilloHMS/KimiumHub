package com.proautokimium.api.domain.enums.humanResources;

/**
 * Onde o atestado está na conferência do RH.
 *
 * Recusado não é final: a pessoa reenvia, e o atestado volta a PENDING.
 */
public enum MedicalCertificateStatus {
    /** Chegou e o RH ainda não conferiu. */
    PENDING,
    /** O RH confirmou o recebimento. */
    RECEIVED,
    /** O RH recusou — ilegível, incompleto. Dá para reenviar. */
    REJECTED
}
