package com.proautokimium.api.Application.DTOs.humanResources.MedicalCertificate;

import java.util.List;

public record EmployeeMedicalCertificatesDTO(
        List<MedicalCertificateResponseDTO> history,
        long countThisYear
) {
}
