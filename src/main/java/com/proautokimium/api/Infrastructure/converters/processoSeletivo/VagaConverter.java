package com.proautokimium.api.Infrastructure.converters.processoSeletivo;

import com.proautokimium.api.Application.DTOs.processoSeletivo.vaga.CreateVagaDTO;
import com.proautokimium.api.Application.DTOs.processoSeletivo.vaga.ResponseVagaDTO;
import com.proautokimium.api.Application.DTOs.processoSeletivo.vaga.UpdateVagaDTO;
import com.proautokimium.api.Infrastructure.interfaces.converters.DtoConverter;
import com.proautokimium.api.Infrastructure.interfaces.converters.UpdateDtoConverter;
import com.proautokimium.api.domain.entities.processoSeletivo.Vaga;
import com.proautokimium.api.domain.enums.processoSeletivo.StatusVaga;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

@Component
public class VagaConverter implements DtoConverter<Vaga, ResponseVagaDTO, CreateVagaDTO>, UpdateDtoConverter<Vaga, UpdateVagaDTO> {
    @Override
    public ResponseVagaDTO toDto(Vaga entity) {
        return new ResponseVagaDTO(
                entity.getId(),
                entity.getTitulo(),
                entity.getDescricao(),
                entity.getRequisitos(),
                entity.getBeneficios(),
                entity.getArea(),
                entity.getDataAbertura(),
                entity.getDataEncerramento()
        );
    }

    @Override
    public Vaga fromCreateDto(CreateVagaDTO dto) {
        Vaga vaga = new Vaga();
        vaga.setTitulo(dto.titulo());
        vaga.setDescricao(dto.descricao());
        vaga.setRequisitos(dto.requisitos());
        vaga.setBeneficios(dto.beneficios());
        vaga.setArea(dto.area());
        vaga.setDataAbertura(dto.dataAbertura() == null ? LocalDateTime.now() : dto.dataAbertura());
        vaga.setDataEncerramento(dto.dataEncerramento());
        vaga.setStatus(StatusVaga.RASCUNHO);

        return vaga;
    }

    @Override
    public void updateFromDto(UpdateVagaDTO dto, Vaga entity) {
        entity.setTitulo(dto.titulo());
        entity.setDescricao(dto.descricao());
        entity.setRequisitos(dto.requisitos());
        entity.setBeneficios(dto.beneficios());
        entity.setArea(dto.area());
        entity.setDataEncerramento(dto.dataEncerramento());
    }
}
