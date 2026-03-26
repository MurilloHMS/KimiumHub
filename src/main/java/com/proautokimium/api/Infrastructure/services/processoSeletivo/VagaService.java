package com.proautokimium.api.Infrastructure.services.processoSeletivo;

import com.proautokimium.api.Application.DTOs.processoSeletivo.vaga.CreateVagaDTO;
import com.proautokimium.api.Application.DTOs.processoSeletivo.vaga.ResponseVagaDTO;
import com.proautokimium.api.Application.DTOs.processoSeletivo.vaga.UpdateVagaDTO;
import com.proautokimium.api.Infrastructure.exceptions.processoSeletivo.VagaNotExistsException;
import com.proautokimium.api.Infrastructure.repositories.processoSeletivo.VagaRepository;
import com.proautokimium.api.domain.entities.processoSeletivo.Vaga;
import com.proautokimium.api.domain.enums.processoSeletivo.StatusVaga;
import jakarta.transaction.Transactional;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Service
public class VagaService {

    private final VagaRepository vagaRepository;

    public VagaService(VagaRepository vagaRepository) {
        this.vagaRepository = vagaRepository;
    }

    public void create(CreateVagaDTO dto) {
        Vaga vaga = new Vaga();
        vaga.fromDTO(dto);
        vagaRepository.save(vaga);
    }

    public void update(UpdateVagaDTO dto){
        Vaga vaga = vagaRepository.findById(dto.id()).orElseThrow(VagaNotExistsException::new);
        vaga.fromDTO(dto);
        vagaRepository.save(vaga);
    }

    public void publicar(UUID id) {
        Vaga vaga = vagaRepository.findById(id).orElseThrow(VagaNotExistsException::new);
        vaga.publicar();
        vagaRepository.save(vaga);
    }

    public void rascunho(UUID id) {
        Vaga vaga = vagaRepository.findById(id).orElseThrow(VagaNotExistsException::new);
        vaga.rascunho();
        vagaRepository.save(vaga);
    }

    public void arquivar(UUID id) {
       Vaga vaga = vagaRepository.findById(id).orElseThrow(VagaNotExistsException::new);
        vaga.arquivar();
        vagaRepository.save(vaga);
    }

    public void encerrar(UUID id) {
        Vaga vaga = vagaRepository.findById(id).orElseThrow(VagaNotExistsException::new);
        vaga.encerrar();
        vagaRepository.save(vaga);
    }

    public List<ResponseVagaDTO> listarVagasPublicadas() {
        List<Vaga> byStatus = this.vagaRepository.findByStatus(StatusVaga.PUBLICADA);
        return byStatus.stream().map(Vaga::toDTO).toList();
    }

    public List<ResponseVagaDTO> listarVagasEncerrados() {
        List<Vaga> byStatus = this.vagaRepository.findByStatus(StatusVaga.ENCERRADA);
        return byStatus.stream().map(Vaga::toDTO).toList();
    }

    public List<ResponseVagaDTO> listarVagasEmRascunho() {
        List<Vaga> byStatus = this.vagaRepository.findByStatus(StatusVaga.RASCUNHO);
        return byStatus.stream().map(Vaga::toDTO).toList();
    }

    public List<ResponseVagaDTO> listarVagasArquivadas() {
        List<Vaga> byStatus = this.vagaRepository.findByStatus(StatusVaga.ARQUIVADA);
        return byStatus.stream().map(Vaga::toDTO).toList();
    }
}
