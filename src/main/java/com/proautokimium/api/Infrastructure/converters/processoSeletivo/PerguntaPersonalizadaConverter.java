package com.proautokimium.api.Infrastructure.converters.processoSeletivo;

import com.proautokimium.api.Application.DTOs.processoSeletivo.perguntas.CreatePerguntaDTO;
import com.proautokimium.api.Application.DTOs.processoSeletivo.perguntas.ResponsePerguntaPersonalizadaDTO;
import com.proautokimium.api.Application.DTOs.processoSeletivo.perguntas.UpdatePerguntaDTO;
import com.proautokimium.api.Infrastructure.interfaces.converters.DtoConverter;
import com.proautokimium.api.Infrastructure.interfaces.converters.UpdateDtoConverter;
import com.proautokimium.api.domain.entities.processoSeletivo.PerguntaPersonalizada;
import org.springframework.stereotype.Component;

@Component
public class PerguntaPersonalizadaConverter implements DtoConverter<PerguntaPersonalizada, ResponsePerguntaPersonalizadaDTO, CreatePerguntaDTO>, UpdateDtoConverter<PerguntaPersonalizada, UpdatePerguntaDTO> {
    @Override
    public ResponsePerguntaPersonalizadaDTO toDto(PerguntaPersonalizada entity) {
        return new ResponsePerguntaPersonalizadaDTO(
                entity.getId(),
                entity.getEnunciado(),
                entity.getTipo(),
                entity.getObrigatoria(),
                entity.getOrdem()
        );
    }

    @Override
    public PerguntaPersonalizada fromCreateDto(CreatePerguntaDTO dto) {
        return null;
    }

    @Override
    public void updateFromDto(UpdatePerguntaDTO dto, PerguntaPersonalizada entity) {
        if(dto == null || entity == null) return;

        entity.setEnunciado(dto.enunciado());
        entity.setTipo(dto.tipo());
        entity.setObrigatoria(dto.obrigatoria());
        entity.setOrdem(dto.ordem());
    }
}
