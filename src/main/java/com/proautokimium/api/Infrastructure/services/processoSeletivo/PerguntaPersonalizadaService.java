package com.proautokimium.api.Infrastructure.services.processoSeletivo;

import com.proautokimium.api.Application.DTOs.processoSeletivo.perguntas.CreatePerguntaDTO;
import com.proautokimium.api.Application.DTOs.processoSeletivo.perguntas.ResponsePerguntaPersonalizadaDTO;
import com.proautokimium.api.Application.DTOs.processoSeletivo.perguntas.UpdatePerguntaDTO;
import com.proautokimium.api.Infrastructure.exceptions.processoSeletivo.PerguntaNotFoundExeption;
import com.proautokimium.api.Infrastructure.exceptions.processoSeletivo.VagaNotFoundException;
import com.proautokimium.api.Infrastructure.repositories.processoSeletivo.PerguntaPersonalizadaRepository;
import com.proautokimium.api.Infrastructure.repositories.processoSeletivo.VagaRepository;
import com.proautokimium.api.domain.entities.processoSeletivo.PerguntaPersonalizada;
import com.proautokimium.api.domain.entities.processoSeletivo.Vaga;
import jakarta.transaction.Transactional;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Service
public class PerguntaPersonalizadaService {

    private final PerguntaPersonalizadaRepository perguntaPersonalizadaRepository;
    private final VagaRepository vagaRepository;

    public  PerguntaPersonalizadaService(PerguntaPersonalizadaRepository perguntaPersonalizadaRepository, VagaRepository vagaRepository) {
        this.perguntaPersonalizadaRepository = perguntaPersonalizadaRepository;
        this.vagaRepository = vagaRepository;
    }

    @Transactional
    public void create(CreatePerguntaDTO dto, UUID vagaID){
        Vaga vaga = vagaRepository.findById(vagaID)
                .orElseThrow(VagaNotFoundException::new);

        PerguntaPersonalizada pp = new PerguntaPersonalizada();
        pp.fromDTO(dto);
        pp.setVaga(vaga);
        perguntaPersonalizadaRepository.save(pp);
    }

    @Transactional
    public void update(UpdatePerguntaDTO dto){
        PerguntaPersonalizada pergunta = perguntaPersonalizadaRepository.findById(dto.id())
                .orElseThrow(PerguntaNotFoundExeption::new);

        pergunta.fromDTO(dto);
        perguntaPersonalizadaRepository.save(pergunta);
    }

    @Transactional
    public void delete(UUID id){
        perguntaPersonalizadaRepository.deleteById(id);
    }

    public List<ResponsePerguntaPersonalizadaDTO> listarPerguntasPorVaga(UUID vagaID){
        Vaga vaga = vagaRepository.findById(vagaID)
                .orElseThrow(VagaNotFoundException::new);

        return perguntaPersonalizadaRepository.findAllByVaga(vaga)
                .stream().map(PerguntaPersonalizada::toDTO).toList();
    }
}
