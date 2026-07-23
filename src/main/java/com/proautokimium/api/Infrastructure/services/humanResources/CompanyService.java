package com.proautokimium.api.Infrastructure.services.humanResources;

import com.proautokimium.api.Application.DTOs.humanResources.Company.CompanyResponseDTO;
import com.proautokimium.api.Application.DTOs.humanResources.Company.CreateCompanyRequestDTO;
import com.proautokimium.api.Infrastructure.repositories.humanResources.CompanyRepository;
import com.proautokimium.api.domain.entities.humanResources.Company;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class CompanyService {

    private final CompanyRepository companyRepository;

    public CompanyService(CompanyRepository companyRepository) {
        this.companyRepository = companyRepository;
    }

    public CompanyResponseDTO create(CreateCompanyRequestDTO request){
        Company company = new Company(
                request.name(),
                request.legalName(),
                request.cnpj()
        );
        Company saved = companyRepository.save(company);
        return toResponse(saved);
    }

    public List<CompanyResponseDTO> listAll(){
        return companyRepository.findAll().stream()
                .map(this::toResponse)
                .toList();
    }

    private CompanyResponseDTO toResponse(Company company){
        return new CompanyResponseDTO(
                company.getId(),
                company.getName(),
                company.getLegalName(),
                company.getCnpj());
    }
}
