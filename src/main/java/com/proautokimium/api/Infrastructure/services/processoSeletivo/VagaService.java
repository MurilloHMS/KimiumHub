package com.proautokimium.api.Infrastructure.services.processoSeletivo;

import com.proautokimium.api.Application.DTOs.processoSeletivo.vaga.CreateVagaDTO;
import com.proautokimium.api.Application.DTOs.processoSeletivo.vaga.ResponseVagaDTO;
import com.proautokimium.api.Application.DTOs.processoSeletivo.vaga.UpdateVagaDTO;
import com.proautokimium.api.Infrastructure.exceptions.processoSeletivo.VagaNotFoundException;
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

    @Transactional
    public Vaga create(CreateVagaDTO dto) {
        Vaga vaga = new Vaga();
        vaga.fromDTO(dto);
        return vagaRepository.save(vaga);
    }

    @Transactional
    public void update(UpdateVagaDTO dto){
        Vaga vaga = vagaRepository.findById(dto.id()).orElseThrow(VagaNotFoundException::new);
        vaga.fromDTO(dto);
        vagaRepository.save(vaga);
    }

    @Transactional
    public void publicar(UUID id) {
        Vaga vaga = vagaRepository.findById(id).orElseThrow(VagaNotFoundException::new);
        vaga.publicar();
        vagaRepository.save(vaga);
    }

    @Transactional
    public void rascunho(UUID id) {
        Vaga vaga = vagaRepository.findById(id).orElseThrow(VagaNotFoundException::new);
        vaga.rascunho();
        vagaRepository.save(vaga);
    }

    @Transactional
    public void arquivar(UUID id) {
       Vaga vaga = vagaRepository.findById(id).orElseThrow(VagaNotFoundException::new);
        vaga.arquivar();
        vagaRepository.save(vaga);
    }

    @Transactional
    public void encerrar(UUID id) {
        Vaga vaga = vagaRepository.findById(id).orElseThrow(VagaNotFoundException::new);
        vaga.encerrar();
        vagaRepository.save(vaga);
    }

    public ResponseVagaDTO listarVaga(UUID id){
        Vaga vaga = vagaRepository.findById(id).orElseThrow(VagaNotFoundException::new);
        return vaga.toDTO();
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
