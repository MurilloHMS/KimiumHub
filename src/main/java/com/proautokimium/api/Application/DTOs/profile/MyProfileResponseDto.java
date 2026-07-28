package com.proautokimium.api.Application.DTOs.profile;

public record MyProfileResponseDto(
        ProfileResponseDto profile,
        String employeeName,
        String employeeEmail,
        String employeeCargo,
        String employeeEmpresa,
        boolean canCreateProfile
) {
}
