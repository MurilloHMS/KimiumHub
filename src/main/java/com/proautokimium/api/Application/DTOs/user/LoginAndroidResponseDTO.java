package com.proautokimium.api.Application.DTOs.user;

import com.proautokimium.api.Application.DTOs.partners.EmployeeDTO;

public record LoginAndroidResponseDTO(String token, EmployeeDTO employee) {}
