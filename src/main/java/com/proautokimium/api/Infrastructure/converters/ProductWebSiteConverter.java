package com.proautokimium.api.Infrastructure.converters;

import com.proautokimium.api.Application.DTOs.product.ProductWebSiteCreateDTO;
import com.proautokimium.api.Application.DTOs.product.ProductWebSiteResponseDTO;
import com.proautokimium.api.Application.DTOs.product.ProductWebSiteUpdateDTO;
import com.proautokimium.api.Infrastructure.interfaces.converters.DtoConverter;
import com.proautokimium.api.domain.entities.ProductWebsite;
import org.springframework.stereotype.Component;

@Component
public class ProductWebSiteConverter implements DtoConverter<ProductWebsite, ProductWebSiteResponseDTO, ProductWebSiteCreateDTO, ProductWebSiteUpdateDTO> {
    @Override
    public ProductWebSiteResponseDTO toDto(ProductWebsite entity) {
        if(entity == null) return null;

        return new ProductWebSiteResponseDTO(
                entity.getId(),
                entity.getSystemCode(),
                entity.getName(),
                entity.isActive(),
                entity.getCores(),
                entity.getFinalidade(),
                entity.getDiluicao(),
                entity.getDescricao()
        );
    }

    @Override
    public ProductWebsite fromCreateDto(ProductWebSiteCreateDTO dto) {
        if(dto == null) return null;

        ProductWebsite entity = new ProductWebsite();
        entity.setName(dto.name());
        entity.setDescricao(dto.descricao());
        entity.setCores(dto.cores());
        entity.setFinalidade(dto.finalidade());
        entity.setSystemCode(dto.systemCode());
        entity.setActive(dto.active());
        entity.setDiluicao(dto.diluicao());
        return entity;
    }

    @Override
    public void updateFromDto(ProductWebSiteUpdateDTO dto, ProductWebsite entity) {
        if(dto == null || entity == null) return;

        entity.setName(dto.name());
        entity.setActive(dto.active());
        entity.setCores(dto.cores());
        entity.setFinalidade(dto.finalidade());
        entity.setDiluicao(dto.diluicao());
        entity.setDescricao(dto.descricao());
    }
}
