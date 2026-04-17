package com.proautokimium.api.Infrastructure.services.processoSeletivo;

import com.proautokimium.api.Application.DTOs.processoSeletivo.candidaturas.CreateCandidaturaDTO;
import com.proautokimium.api.Application.DTOs.processoSeletivo.candidaturas.ResponseCandidaturaDTO;
import com.proautokimium.api.Infrastructure.exceptions.processoSeletivo.VagaNotFoundException;
import com.proautokimium.api.Infrastructure.repositories.processoSeletivo.CandidatoRepository;
import com.proautokimium.api.Infrastructure.repositories.processoSeletivo.CandidaturaRepository;
import com.proautokimium.api.Infrastructure.repositories.processoSeletivo.VagaRepository;
import com.proautokimium.api.domain.entities.processoSeletivo.Candidato;
import com.proautokimium.api.domain.entities.processoSeletivo.Candidatura;
import com.proautokimium.api.domain.entities.processoSeletivo.Vaga;

import com.proautokimium.api.domain.valueObjects.Email;
import jakarta.transaction.Transactional;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;
import java.util.UUID;

@Service
public class CandidaturaService {

    private final CandidatoRepository candidatoRepository;
    private final CandidaturaRepository candidaturaRepository;
    private final VagaRepository vagaRepository;
    private final StorageService storageService;

    public CandidaturaService(CandidatoRepository candidatoRepository, CandidaturaRepository candidaturaRepository, VagaRepository vagaRepository, StorageService storageService) {
        this.candidatoRepository = candidatoRepository;
        this.candidaturaRepository = candidaturaRepository;
        this.vagaRepository = vagaRepository;
        this.storageService = storageService;
    }

    public List<ResponseCandidaturaDTO> getCandidaturaByVagaId(UUID vagaId) {
        vagaRepository.findById(vagaId).orElseThrow(VagaNotFoundException::new);

        return candidaturaRepository.findCandidaturasByVagaId(vagaId)
                .stream()
                .map(Candidatura::toDTO)
                .toList();
    }

    @Transactional
    public void create(CreateCandidaturaDTO dto, MultipartFile curriculo) throws IOException {

        Candidato candidato = candidatoRepository.findByEmail(new Email(dto.email()))
                .orElseGet(() -> {
                    Candidato novo = new Candidato();
                    novo.setNome(dto.nome());
                    novo.setEmail(new Email(dto.email()));
                    novo.setTelefone(dto.telefone());
                    novo.setUrlLinkedin(dto.urlLinkedin());
                    return candidatoRepository.save(novo);
                });

        if (curriculo != null && !curriculo.isEmpty()) {
            String nomeArquivo = storageService.salvarCurriculo(curriculo, candidato.getId());
            candidato.setPathCurriculo(nomeArquivo);
            candidatoRepository.save(candidato);
        }

        Vaga vaga = vagaRepository.findById(dto.vagaID())
                .orElseThrow(VagaNotFoundException::new);

        if (candidaturaRepository.existsByCandidatoAndVaga(candidato, vaga)) {
            throw new RuntimeException("Candidato já se candidatou para essa vaga");
        }

        Candidatura candidatura = new Candidatura();
        candidatura.setCandidato(candidato);
        candidatura.setVaga(vaga);
        candidatura.iniciar();

        candidaturaRepository.save(candidatura);
    }

    @Transactional
    public void avancarEtapa(UUID id){
        Candidatura candidatura = candidaturaRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Candidatura não encontrada"));

        candidatura.avancarEtapa();
        candidaturaRepository.save(candidatura);
    }

    @Transactional
    public void aprovarCandidatura(UUID id){
        Candidatura candidatura = candidaturaRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Candidatura não encontrada"));

        candidatura.aprovar();
        candidaturaRepository.save(candidatura);
    }

    @Transactional
    public void reprovarCandidatura(UUID id){
        Candidatura candidatura = candidaturaRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Candidatura não encontrada"));

        candidatura.reprovar();
        candidaturaRepository.save(candidatura);
    }

    @Transactional
    public void encerrarCandidatura(UUID id){
        Candidatura candidatura = candidaturaRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Candidatura não encontrada"));

        candidatura.encerrar();
        candidaturaRepository.save(candidatura);
    }
}
