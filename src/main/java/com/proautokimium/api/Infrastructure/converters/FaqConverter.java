package com.proautokimium.api.Infrastructure.converters;

import com.proautokimium.api.Application.DTOs.faq.FaqCreateDTO;
import com.proautokimium.api.Application.DTOs.faq.FaqPublicResponseDTO;
import com.proautokimium.api.Application.DTOs.faq.FaqResponseDTO;
import com.proautokimium.api.Application.DTOs.faq.FaqUpdateDTO;
import com.proautokimium.api.Infrastructure.interfaces.converters.DtoConverter;
import com.proautokimium.api.Infrastructure.interfaces.converters.UpdateDtoConverter;
import com.proautokimium.api.domain.entities.Faq;
import org.springframework.stereotype.Component;

@Component
public class FaqConverter implements DtoConverter<Faq, FaqResponseDTO, FaqCreateDTO>, UpdateDtoConverter<Faq, FaqUpdateDTO> {
    @Override
    public FaqResponseDTO toDto(Faq entity) {
        return new FaqResponseDTO(
                entity.getId(),
                entity.getTitle(),
                entity.getBody(),
                entity.getStatus());
    }

    public FaqPublicResponseDTO toPublicDto(Faq entity){
        return new FaqPublicResponseDTO(
                entity.getTitle(),
                entity.getBody(),
                entity.getStatus());
    }

    @Override
    public Faq fromCreateDto(FaqCreateDTO dto) {
        Faq faq = new Faq();
        faq.setTitle(dto.title());
        faq.setBody(dto.body());
        return faq;
    }

    @Override
    public void updateFromDto(FaqUpdateDTO dto, Faq entity) {
        entity.setTitle(dto.title());
        entity.setBody(dto.body());
    }
}
