package com.proautokimium.api.Infrastructure.converters.processoSeletivo;

import com.proautokimium.api.Application.DTOs.processoSeletivo.candidaturas.CreateCandidaturaDTO;
import com.proautokimium.api.Application.DTOs.processoSeletivo.candidaturas.ResponseCandidaturaDTO;
import com.proautokimium.api.Infrastructure.interfaces.converters.DtoConverter;
import com.proautokimium.api.domain.entities.processoSeletivo.Candidatura;
import org.springframework.stereotype.Component;

@Component
public class CandidaturaConverter implements DtoConverter<Candidatura, ResponseCandidaturaDTO, CreateCandidaturaDTO> {
    @Override
    public ResponseCandidaturaDTO toDto(Candidatura entity) {
        return new ResponseCandidaturaDTO(
                entity.getId(),
                entity.getCandidato().getNome(),
                entity.getCandidato().getEmail().getAddress(),
                entity.getCandidato().getTelefone(),
                entity.getCandidato().getUrlLinkedin(),
                entity.getCandidato().getPathCurriculo(),
                entity.getVaga().getTitulo(),
                entity.getEtapaAtual(),
                entity.getStatus(),
                entity.getCriadoEm(),
                entity.getAtualizadoEm()
        );
    }

    @Override
    public Candidatura fromCreateDto(CreateCandidaturaDTO dto) {
        return null;
    }
}
