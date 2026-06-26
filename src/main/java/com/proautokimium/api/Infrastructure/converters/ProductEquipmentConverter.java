package com.proautokimium.api.Infrastructure.converters;

import com.proautokimium.api.Application.DTOs.product.equipment.ProductEquipmentCreateDTO;
import com.proautokimium.api.Application.DTOs.product.equipment.ProductEquipmentResponseDTO;
import com.proautokimium.api.Infrastructure.interfaces.converters.DtoConverter;
import com.proautokimium.api.domain.entities.EquipmentGuide;
import org.springframework.stereotype.Component;

@Component
public class ProductEquipmentConverter implements DtoConverter<EquipmentGuide, ProductEquipmentResponseDTO, ProductEquipmentCreateDTO> {
    @Override
    public ProductEquipmentResponseDTO toDto(EquipmentGuide entity) {
        return new ProductEquipmentResponseDTO(
                entity.getId(),
                entity.getNome(),
                entity.getImagem()
        );
    }

    @Override
    public EquipmentGuide fromCreateDto(ProductEquipmentCreateDTO dto) {
        EquipmentGuide equipment = new EquipmentGuide();
        equipment.setNome(dto.nome());

        return equipment;
    }
}
