package com.proautokimium.api.Infrastructure.services.processoSeletivo;

import com.proautokimium.api.Application.DTOs.processoSeletivo.candidaturas.CreateCandidaturaDTO;
import com.proautokimium.api.Application.DTOs.processoSeletivo.candidaturas.ResponseCandidaturaDTO;
import com.proautokimium.api.Infrastructure.converters.processoSeletivo.CandidaturaConverter;
import com.proautokimium.api.Infrastructure.exceptions.processoSeletivo.CandidatoAlreadyExistsException;
import com.proautokimium.api.Infrastructure.exceptions.processoSeletivo.CandidaturaAlreadyExistsException;
import com.proautokimium.api.Infrastructure.exceptions.processoSeletivo.VagaNotFoundException;
import com.proautokimium.api.Infrastructure.factories.EmailFactory;
import com.proautokimium.api.Infrastructure.repositories.processoSeletivo.CandidatoRepository;
import com.proautokimium.api.Infrastructure.repositories.processoSeletivo.CandidaturaRepository;
import com.proautokimium.api.Infrastructure.repositories.processoSeletivo.VagaRepository;
import com.proautokimium.api.Infrastructure.services.email.EmailQueueService;
import com.proautokimium.api.Infrastructure.services.storage.StorageService;
import com.proautokimium.api.domain.entities.email.EmailQueue;
import com.proautokimium.api.domain.entities.processoSeletivo.Candidato;
import com.proautokimium.api.domain.entities.processoSeletivo.Candidatura;
import com.proautokimium.api.domain.entities.processoSeletivo.Vaga;

import com.proautokimium.api.domain.enums.processoSeletivo.Etapa;
import com.proautokimium.api.domain.valueObjects.Email;
import jakarta.transaction.Transactional;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
public class CandidaturaService {

    private final CandidatoRepository candidatoRepository;
    private final CandidaturaRepository candidaturaRepository;
    private final VagaRepository vagaRepository;
    private final StorageService storageService;
    private final EmailQueueService emailService;
    private final EmailFactory emailFactory;
    private final CandidaturaConverter converter;
    private final Clock clock;

    public CandidaturaService(CandidatoRepository candidatoRepository, CandidaturaRepository candidaturaRepository, VagaRepository vagaRepository, StorageService storageService, EmailQueueService emailService, EmailFactory emailFactory, CandidaturaConverter converter, Clock clock) {
        this.candidatoRepository = candidatoRepository;
        this.candidaturaRepository = candidaturaRepository;
        this.vagaRepository = vagaRepository;
        this.storageService = storageService;
        this.emailService = emailService;
        this.emailFactory = emailFactory;
        this.converter = converter;
        this.clock = clock;
    }

    public List<ResponseCandidaturaDTO> getCandidaturaByVagaId(UUID vagaId) {
        vagaRepository.findById(vagaId).orElseThrow(VagaNotFoundException::new);

        return candidaturaRepository.findCandidaturasByVagaId(vagaId)
                .stream()
                .map(converter::toDto)
                .toList();
    }

    /**
     * Recebe uma candidatura do formulário público.
     *
     * <p><b>A ordem dos passos é a correção de um defeito, não estilo.</b> Até
     * 2026-09-11 o currículo era gravado <i>antes</i> da checagem de duplicata.
     * Como o nome do arquivo é fixo ({@code <candidatoId>.<ext>}) e a gravação
     * usa {@code REPLACE_EXISTING}, um duplo clique sobrescrevia o currículo bom
     * e só depois lançava 409.
     *
     * <p>E <b>{@code @Transactional} não salva disso</b>: escrita em filesystem
     * não participa da transação e não faz rollback. Quem protege é a ordem —
     * resolver a vaga, resolver o candidato, recusar a duplicata, e só então
     * tocar no disco.
     */
    @Transactional
    public void create(CreateCandidaturaDTO dto, MultipartFile curriculo) throws IOException {

        Vaga vaga = vagaRepository.findById(dto.vagaID())
                .orElseThrow(VagaNotFoundException::new);

        Candidato candidato = candidatoRepository
                .findByEmail_AddressIgnoreCase(normalizar(dto.email()))
                .map(existente -> atualizarDados(existente, dto))
                .orElseGet(() -> novoCandidato(dto));

        if (candidaturaRepository.existsByCandidatoAndVaga(candidato, vaga)) {
            throw new CandidaturaAlreadyExistsException("Candidato já se candidatou para essa vaga");
        }

        if (curriculo != null && !curriculo.isEmpty()) {
            String nomeArquivo = storageService.save(curriculo, candidato.getId().toString());
            candidato.setPathCurriculo(nomeArquivo);
            candidatoRepository.save(candidato);
        }

        Candidatura candidatura = new Candidatura();
        candidatura.setCandidato(candidato);
        candidatura.setVaga(vaga);
        candidatura.iniciar();

        Candidatura saved = candidaturaRepository.save(candidatura);

        EmailQueue emailQueue = emailFactory.candidaturaConfirmada(
                saved.getCandidato().getEmail().getAddress(),
                saved.getCandidato().getNome(),
                saved.getVaga().getTitulo());
        emailService.create(emailQueue);
    }

    /**
     * Reenviar atualiza os dados.
     *
     * <p>Antes de 2026-09-11 este ramo descartava em silêncio o que a pessoa
     * tinha acabado de digitar: o cadastro antigo vencia o formulário novo, e o
     * RH ligava para o telefone de dois anos atrás.
     *
     * <p>O e-mail <b>não</b> é atualizado — ele é a identidade da linha, e é o
     * que casou esta busca.
     */
    private Candidato atualizarDados(Candidato candidato, CreateCandidaturaDTO dto) {
        candidato.setNome(dto.nome());
        candidato.setTelefone(dto.telefone());
        candidato.setUrlLinkedin(dto.urlLinkedin());
        candidatoRepository.save(candidato);
        // Devolve a instância que já temos, e não o retorno do save: a entidade
        // veio gerenciada do repositório, e reatribuir só acrescenta um jeito
        // de o objeto virar outro sem ninguém perceber.
        return candidato;
    }

    /**
     * O {@code criadoEm} sai daqui, e não do banco.
     *
     * <p>A coluna tem {@code DEFAULT CURRENT_TIMESTAMP} desde a V35 e o default
     * <b>nunca dispara</b>: sem {@code @DynamicInsert}, o Hibernate monta um
     * INSERT estático com todas as colunas e emite {@code NULL}. Nenhum
     * {@code ALTER ... SET DEFAULT} conserta isso.
     */
    private Candidato novoCandidato(CreateCandidaturaDTO dto) {
        Candidato novo = new Candidato();
        novo.setNome(dto.nome());
        novo.setEmail(new Email(normalizar(dto.email())));
        novo.setTelefone(dto.telefone());
        novo.setUrlLinkedin(dto.urlLinkedin());
        novo.setCriadoEm(LocalDateTime.now(clock));
        return candidatoRepository.save(novo);
    }

    /** Um e-mail, uma pessoa: o índice único da V103 é sobre {@code lower(email)}. */
    private static String normalizar(String email) {
        return email == null ? null : email.trim().toLowerCase();
    }

    @Transactional
    public void avancarEtapa(UUID id){
        Candidatura candidatura = candidaturaRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Candidatura não encontrada"));

        candidatura.avancarEtapa();
        Candidatura saved = candidaturaRepository.save(candidatura);
        enviarEmailEtapa(saved);
    }

    @Transactional
    public void aprovarCandidatura(UUID id){
        Candidatura candidatura = candidaturaRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Candidatura não encontrada"));

        candidatura.aprovar();
        Candidatura saved = candidaturaRepository.save(candidatura);
        EmailQueue emailQueue = emailFactory.candidaturaAprovada(
                saved.getCandidato().getEmail().getAddress(),
                saved.getCandidato().getNome(),
                saved.getVaga().getTitulo());
        emailService.create(emailQueue);
    }

    @Transactional
    public void reprovarCandidatura(UUID id){
        Candidatura candidatura = candidaturaRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Candidatura não encontrada"));

        candidatura.reprovar();
        Candidatura saved = candidaturaRepository.save(candidatura);
        EmailQueue emailQueue = emailFactory.candidaturaReprovada(
                saved.getCandidato().getEmail().getAddress(),
                saved.getCandidato().getNome(),
                saved.getVaga().getTitulo());
        emailService.create(emailQueue);
    }

    @Transactional
    public void encerrarCandidatura(UUID id){
        Candidatura candidatura = candidaturaRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Candidatura não encontrada"));

        candidatura.encerrar();
        Candidatura saved = candidaturaRepository.save(candidatura);
        EmailQueue emailQueue = emailFactory.candidaturaReprovada(
                saved.getCandidato().getEmail().getAddress(),
                saved.getCandidato().getNome(),
                saved.getVaga().getTitulo());
        emailService.create(emailQueue);
    }

    private void enviarEmailEtapa(Candidatura candidatura){

        EmailQueue email;

        if(candidatura.getEtapaAtual() == Etapa.CONTRATADO){

            email = emailFactory.candidaturaAprovada(
                    candidatura.getCandidato().getEmail().getAddress(),
                    candidatura.getCandidato().getNome(),
                    candidatura.getVaga().getTitulo()
            );

        } else {

            email = emailFactory.avancoEtapa(
                    candidatura.getCandidato().getEmail().getAddress(),
                    candidatura.getCandidato().getNome(),
                    candidatura.getVaga().getTitulo()
            );
        }

        emailService.create(email);
    }
}
