package com.proautokimium.api.Application.DTOs.humanResources.Company;

import com.proautokimium.api.Application.DTOs.address.AddressDTO;
import jakarta.validation.Valid;

/**
 * Criar e editar usam o mesmo corpo: não há campo que só um dos dois aceite.
 *
 * @param address opcional
 */
public record CreateCompanyRequestDTO(String name, String legalName, String cnpj, @Valid AddressDTO address) {
}
