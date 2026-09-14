package com.proautokimium.api.Application.DTOs.humanResources.Company;

import com.proautokimium.api.Application.DTOs.address.AddressDTO;

import java.util.UUID;

/** @param address nulo quando a empresa ainda não tem endereço. */
public record CompanyResponseDTO(UUID id, String name, String legalName, String cnpj, AddressDTO address) {
}
