package com.proautokimium.api.Infrastructure.services.humanResources;

import com.proautokimium.api.Application.DTOs.address.AddressDTO;
import com.proautokimium.api.Application.DTOs.humanResources.Company.CompanyResponseDTO;
import com.proautokimium.api.Application.DTOs.humanResources.Company.CreateCompanyRequestDTO;
import com.proautokimium.api.Infrastructure.exceptions.humanResources.CompanyNotFoundException;
import com.proautokimium.api.Infrastructure.repositories.humanResources.CompanyRepository;
import com.proautokimium.api.domain.entities.humanResources.Company;
import jakarta.transaction.Transactional;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Service
public class CompanyService {

    private final CompanyRepository companyRepository;

    public CompanyService(CompanyRepository companyRepository) {
        this.companyRepository = companyRepository;
    }

    public CompanyResponseDTO create(CreateCompanyRequestDTO request){
        Company company = new Company();
        apply(company, request);
        Company saved = companyRepository.save(company);
        return toResponse(saved);
    }

    /**
     * Editar não existia até 2026-09-14: a tela só criava, e empresa digitada
     * errada ficava errada. Entrou junto com o endereço, que é o que se vai
     * querer corrigir depois de criar.
     */
    @Transactional
    public CompanyResponseDTO update(UUID id, CreateCompanyRequestDTO request) {
        Company company = companyRepository.findById(id).orElseThrow(CompanyNotFoundException::new);
        apply(company, request);
        return toResponse(companyRepository.save(company));
    }

    public List<CompanyResponseDTO> listAll(){
        return companyRepository.findAll().stream()
                .map(this::toResponse)
                .toList();
    }

    private static void apply(Company company, CreateCompanyRequestDTO request) {
        company.setName(request.name());
        company.setLegalName(request.legalName());
        company.setCnpj(request.cnpj());
        company.setAddress(request.address() == null ? null : request.address().toAddress());
    }

    private CompanyResponseDTO toResponse(Company company){
        return new CompanyResponseDTO(
                company.getId(),
                company.getName(),
                company.getLegalName(),
                company.getCnpj(),
                AddressDTO.from(company.getAddress()));
    }
}
