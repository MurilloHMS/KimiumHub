package com.proautokimium.api.Infrastructure.services.processoSeletivo;

import com.proautokimium.api.Application.DTOs.processoSeletivo.candidato.CreateCandidatoDTO;
import com.proautokimium.api.Application.DTOs.processoSeletivo.candidato.ResponseCandidatoDTO;
import com.proautokimium.api.Infrastructure.exceptions.processoSeletivo.CandidatoAlreadyExistsException;
import com.proautokimium.api.Infrastructure.repositories.processoSeletivo.CandidatoRepository;
import com.proautokimium.api.domain.entities.processoSeletivo.Candidato;
import com.proautokimium.api.domain.valueObjects.Email;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class CandidatoService {

    private final CandidatoRepository candidatoRepository;

    public  CandidatoService(CandidatoRepository candidatoRepository) {
        this.candidatoRepository = candidatoRepository;
    }

    public Candidato create(CreateCandidatoDTO dto){
        if(candidatoRepository.findByEmail(new Email(dto.email())).isPresent())
            throw new CandidatoAlreadyExistsException();

        Candidato candidato =  new Candidato();
        candidato.fromDTO(dto);
        return candidatoRepository.save(candidato);
    }

    public List<ResponseCandidatoDTO> listarCandidatos(){
        return candidatoRepository.findAll().stream().map(Candidato::toDTO).toList();
    }
}
