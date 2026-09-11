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
import java.util.Optional;
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
    private final String websiteBaseUrl;
    private final int mesesDeRetencao;

    private static final java.time.format.DateTimeFormatter DATA =
            java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy");

    public CandidaturaService(CandidatoRepository candidatoRepository, CandidaturaRepository candidaturaRepository, VagaRepository vagaRepository, StorageService storageService, EmailQueueService emailService, EmailFactory emailFactory, CandidaturaConverter converter, Clock clock,
                              @org.springframework.beans.factory.annotation.Value("${app.base-url}") String websiteBaseUrl,
                              @org.springframework.beans.factory.annotation.Value("${talent-bank.retention-months:24}") int mesesDeRetencao) {
        this.candidatoRepository = candidatoRepository;
        this.candidaturaRepository = candidaturaRepository;
        this.vagaRepository = vagaRepository;
        this.storageService = storageService;
        this.emailService = emailService;
        this.emailFactory = emailFactory;
        this.converter = converter;
        this.clock = clock;
        this.websiteBaseUrl = websiteBaseUrl;
        this.mesesDeRetencao = mesesDeRetencao;
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

        LocalDateTime agora = LocalDateTime.now(clock);
        boolean enviouArquivo = curriculo != null && !curriculo.isEmpty();

        Optional<Candidato> jaCadastrado =
                candidatoRepository.findByEmail_AddressIgnoreCase(normalizar(dto.email()));

        // Lidos ANTES de qualquer escrita: depois de atualizar, a data vira
        // "agora" e o aviso do e-mail passaria a mentir.
        boolean tinhaCurriculo = jaCadastrado.map(c -> c.getPathCurriculo() != null).orElse(false);
        LocalDateTime dataDoCurriculo = jaCadastrado.map(CandidaturaService::ultimaAtualizacaoDe).orElse(null);

        Candidato candidato = jaCadastrado
                .map(existente -> atualizarDados(existente, dto, agora))
                .orElseGet(() -> novoCandidato(dto, agora));

        if (candidaturaRepository.existsByCandidatoAndVaga(candidato, vaga)) {
            throw new CandidaturaAlreadyExistsException("Candidato já se candidatou para essa vaga");
        }

        if (enviouArquivo) {
            String nomeArquivo = storageService.save(curriculo, candidato.getId().toString());
            candidato.setPathCurriculo(nomeArquivo);
            candidatoRepository.save(candidato);
        }

        // Marcar a caixa aqui e OPCIONAL: candidatar-se a uma vaga e finalidade
        // legitima por si so. O que o aceite acrescenta e permanecer disponivel
        // para vagas futuras.
        if (dto.consentimento()) {
            candidato.registrarConsentimento(agora, mesesDeRetencao);
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
                saved.getVaga().getTitulo(),
                avisoSobreOCurriculo(enviouArquivo, jaCadastrado.isPresent(),
                        tinhaCurriculo, dataDoCurriculo));
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
    private Candidato atualizarDados(Candidato candidato, CreateCandidaturaDTO dto, LocalDateTime agora) {
        candidato.setNome(dto.nome());
        candidato.setTelefone(dto.telefone());
        candidato.setUrlLinkedin(dto.urlLinkedin());
        candidato.setAtualizadoEm(agora);
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
    private Candidato novoCandidato(CreateCandidaturaDTO dto, LocalDateTime agora) {
        Candidato novo = new Candidato();
        novo.setNome(dto.nome());
        novo.setEmail(new Email(normalizar(dto.email())));
        novo.setTelefone(dto.telefone());
        novo.setUrlLinkedin(dto.urlLinkedin());
        novo.setCriadoEm(agora);
        return candidatoRepository.save(novo);
    }

    /**
     * O que o e-mail de confirmacao diz sobre o curriculo.
     *
     * <p><b>A escolha "usar o que ja tenho" ou "mandar outro" e expressa por
     * anexar ou nao o arquivo</b>, e nao por um radio-button. Um radio
     * informado teria que dizer a pessoa o que esta guardado, e essa frase,
     * numa rota publica, e um oraculo: submeta com o e-mail de alguem e
     * descubra se ele procura emprego aqui.
     *
     * <p>O ramo que mais engana e o ultimo. Quando o e-mail e novo e nao veio
     * arquivo, o reflexo e responder <i>400 "anexe seu curriculo"</i> -- e esse
     * 400 vaza o inverso: a <b>ausencia</b> de erro passa a significar "voce
     * esta na base", com uma requisicao e sem upload nenhum. Por isso a
     * candidatura e aceita assim mesmo, com {@code path_curriculo} nulo, que o
     * kanban ja sabe mostrar.
     */
    private String avisoSobreOCurriculo(boolean enviouArquivo, boolean jaCadastrado,
                                        boolean tinhaCurriculo, LocalDateTime dataDoCurriculo) {
        if (enviouArquivo) {
            return jaCadastrado
                    ? paragrafo("Atualizamos seus dados com o que você preencheu agora. "
                              + "Não era isso? " + linkParaOCadastro())
                    : "";
        }

        if (tinhaCurriculo) {
            return paragrafo("Usamos o currículo que você já tinha enviado"
                    + (dataDoCurriculo == null ? "" : " em " + DATA.format(dataDoCurriculo))
                    + ". Quer trocar por outro? " + linkParaOCadastro());
        }

        return paragrafo("Não encontramos um currículo no seu cadastro. "
                + "Você pode enviar um por aqui: " + linkParaOCadastro());
    }

    private String linkParaOCadastro() {
        return "<a href=\"" + websiteBaseUrl + "/meu-curriculo\">acessar meu cadastro</a>";
    }

    private static String paragrafo(String texto) {
        return "<p style=\"background:#f4f4f4; padding:12px 14px; border-radius:8px;\">" + texto + "</p>";
    }

    /**
     * A data que o aviso mostra: a ultima atualizacao, ou a entrada, quando
     * nunca houve atualizacao.
     */
    private static LocalDateTime ultimaAtualizacaoDe(Candidato candidato) {
        return candidato.getAtualizadoEm() != null ? candidato.getAtualizadoEm() : candidato.getCriadoEm();
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
