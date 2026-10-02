package com.proautokimium.api.Application.DTOs.humanResources.MedicalCertificate;

/** Quem confere é sempre o RH autenticado — reviewerId nunca vem do cliente. */
public record ReviewMedicalCertificateDTO(
        String notes
) {
}
