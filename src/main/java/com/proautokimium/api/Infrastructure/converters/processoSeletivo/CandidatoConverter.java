package com.proautokimium.api.Infrastructure.converters.processoSeletivo;

import com.proautokimium.api.Application.DTOs.processoSeletivo.candidato.CreateCandidatoDTO;
import com.proautokimium.api.Application.DTOs.processoSeletivo.candidato.ResponseCandidatoDTO;
import com.proautokimium.api.Infrastructure.interfaces.converters.DtoConverter;
import com.proautokimium.api.domain.entities.processoSeletivo.Candidato;
import com.proautokimium.api.domain.valueObjects.Email;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

@Component
public class CandidatoConverter implements DtoConverter<Candidato, ResponseCandidatoDTO, CreateCandidatoDTO>{
    @Override
    public ResponseCandidatoDTO toDto(Candidato entity) {
        return new ResponseCandidatoDTO(
                entity.getNome(),
                entity.getEmail().getAddress(),
                entity.getTelefone(),
                entity.getUrlLinkedin(),
                entity.getPathCurriculo(),
                entity.getCriadoEm()
        );
    }

    @Override
    public Candidato fromCreateDto(CreateCandidatoDTO dto) {
        Candidato candidato = new Candidato();
        candidato.setNome(dto.nome());
        candidato.setEmail(new Email(dto.email()));
        candidato.setTelefone(dto.telefone());
        candidato.setUrlLinkedin(dto.urlLinkedin());
        candidato.setPathCurriculo(dto.pathCurriculo());
        candidato.setCriadoEm(LocalDateTime.now());
        return candidato;
    }
}
