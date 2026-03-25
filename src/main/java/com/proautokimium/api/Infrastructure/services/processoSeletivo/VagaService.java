package com.proautokimium.api.Infrastructure.services.processoSeletivo;

import com.proautokimium.api.Infrastructure.exceptions.processoSeletivo.VagaAlreadyExistsException;
import com.proautokimium.api.Infrastructure.exceptions.processoSeletivo.VagaNotExistsException;
import com.proautokimium.api.Infrastructure.repositories.processoSeletivo.VagaRepository;
import com.proautokimium.api.domain.entities.processoSeletivo.Vaga;
import com.proautokimium.api.domain.enums.processoSeletivo.StatusVaga;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class VagaService {

    private final VagaRepository vagaRepository;

    public VagaService(VagaRepository vagaRepository) {
        this.vagaRepository = vagaRepository;
    }

    public void create(Vaga vaga) {
        if(this.vagaRepository.existsById(vaga.getId())) {
            throw new VagaAlreadyExistsException();
        }
        this.vagaRepository.save(vaga);
    }

    public void update(Vaga vaga){
        if(!this.vagaRepository.existsById(vaga.getId()))
            throw new VagaNotExistsException();

        this.vagaRepository.save(vaga);
    }

    public void publicar(Vaga vaga) {
        if(!this.vagaRepository.existsById(vaga.getId()))
            throw new VagaNotExistsException();

        vaga.publicar();
        this.vagaRepository.save(vaga);
    }

    public void rascunho(Vaga vaga) {
        if(!this.vagaRepository.existsById(vaga.getId()))
            throw new VagaNotExistsException();

        vaga.rascunho();
        this.vagaRepository.save(vaga);
    }

    public void arquivar(Vaga vaga) {
        if(!this.vagaRepository.existsById(vaga.getId()))
            throw new VagaNotExistsException();

        vaga.arquivar();
        this.vagaRepository.save(vaga);
    }

    public List<Vaga> listarVagasPublicadas() {
        return this.vagaRepository.findByStatus(StatusVaga.PUBLICADA);
    }

    public List<Vaga> listarVagasEncerrados() {
        return this.vagaRepository.findByStatus(StatusVaga.ENCERRADA);
    }

    public List<Vaga> listarVagasEmRascunho() {
        return this.vagaRepository.findByStatus(StatusVaga.RASCUNHO);
    }

    public List<Vaga> listarVagasArquivadas() {
        return this.vagaRepository.findByStatus(StatusVaga.ARQUIVADA);
    }
}
