package com.proautokimium.api.Infrastructure.services.processoSeletivo;

import com.proautokimium.api.Application.DTOs.processoSeletivo.talentBank.CreateTalentBankEntryDTO;
import com.proautokimium.api.Application.DTOs.processoSeletivo.talentBank.TalentBankEntryDTO;
import com.proautokimium.api.Application.DTOs.processoSeletivo.talentBank.TalentBankSummaryDTO;
import com.proautokimium.api.Application.DTOs.processoSeletivo.talentBank.UpdateTalentBankEntryDTO;
import com.proautokimium.api.Infrastructure.exceptions.processoSeletivo.CandidatoNotFoundException;
import com.proautokimium.api.Infrastructure.exceptions.processoSeletivo.CurriculoInvalidoException;
import com.proautokimium.api.Infrastructure.exceptions.processoSeletivo.LinkDeAcessoExpiradoException;
import com.proautokimium.api.Infrastructure.exceptions.processoSeletivo.LinkDeAcessoInvalidoException;
import com.proautokimium.api.Infrastructure.repositories.processoSeletivo.CandidaturaRepository;
import com.proautokimium.api.Infrastructure.repositories.processoSeletivo.CandidatoRepository;
import com.proautokimium.api.Infrastructure.services.email.TalentBankEmailService;
import com.proautokimium.api.Infrastructure.services.storage.StorageService;
import com.proautokimium.api.domain.entities.processoSeletivo.Candidato;
import com.proautokimium.api.domain.entities.processoSeletivo.Candidatura;
import com.proautokimium.api.domain.entities.processoSeletivo.TalentBankAccessToken;
import com.proautokimium.api.domain.valueObjects.Email;
import jakarta.transaction.Transactional;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Path;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/**
 * O banco de talentos.
 *
 * <p><b>Não há entidade nova.</b> Uma inscrição espontânea é um
 * {@link Candidato} sem {@link Candidatura} — o {@code CandidaturaService} já
 * fazia find-or-create por e-mail, e o currículo já era gravado como
 * {@code <id>.<ext>} com substituição. O que faltava era consentimento, prazo,
 * e um jeito de a pessoa voltar.
 *
 * <p><b>A regra que atravessa este arquivo inteiro:</b> nenhuma resposta pode
 * revelar se um e-mail está ou não na base. O dado vazado — "esta pessoa
 * procura emprego" — é sensível, e uma rota pública sem rate limiting é
 * enumerável de graça.
 */
@Service
public class TalentBankService {

    private final CandidatoRepository candidatoRepository;
    private final CandidaturaRepository candidaturaRepository;
    private final StorageService storageService;
    private final TalentBankAccessTokenService tokenService;
    private final TalentBankEmailService emailService;
    private final Clock clock;
    private final int mesesDeRetencao;

    public TalentBankService(CandidatoRepository candidatoRepository,
                             CandidaturaRepository candidaturaRepository,
                             StorageService storageService,
                             TalentBankAccessTokenService tokenService,
                             TalentBankEmailService emailService,
                             Clock clock,
                             @Value("${talent-bank.retention-months:24}") int mesesDeRetencao) {
        this.candidatoRepository = candidatoRepository;
        this.candidaturaRepository = candidaturaRepository;
        this.storageService = storageService;
        this.tokenService = tokenService;
        this.emailService = emailService;
        this.clock = clock;
        this.mesesDeRetencao = mesesDeRetencao;
    }

    // ─── Público ─────────────────────────────────────────────────────────────

    /**
     * Inscrição espontânea: currículo sem vaga aberta.
     *
     * <p><b>E-mail já existente não é erro, e também não sobrescreve.</b> O
     * upload é descartado e a pessoa recebe o mesmo e-mail de link de acesso
     * de sempre.
     *
     * <p>Um {@code 409 "já existe"} seria um oráculo de enumeração. Sobrescrever
     * em silêncio institucionalizaria o defeito que acabamos de corrigir no
     * caminho de candidatura, e apagaria o cadastro de quem errou o endereço.
     *
     * <p>O custo é real e vale dizer na tela: quem já está na base sobe um
     * arquivo à toa e refaz pelo link. É o preço de não ter o oráculo.
     */
    @Transactional
    public void inscrever(CreateTalentBankEntryDTO dto, MultipartFile curriculo) throws IOException {
        if (curriculo == null || curriculo.isEmpty()) {
            // Sem vaga e sem currículo não sobra nada: isto vira agenda de
            // contatos, não banco de talentos.
            throw CurriculoInvalidoException.recusado("Envie um currículo em PDF.");
        }

        LocalDateTime agora = LocalDateTime.now(clock);
        String email = normalizar(dto.email());

        Optional<Candidato> existente = candidatoRepository.findByEmail_AddressIgnoreCase(email);

        Candidato candidato = existente.orElseGet(() -> {
            Candidato novo = new Candidato();
            novo.setNome(dto.nome());
            novo.setEmail(new Email(email));
            novo.setTelefone(dto.telefone());
            novo.setUrlLinkedin(dto.urlLinkedin());
            novo.setAreaInteresse(dto.areaInteresse());
            novo.setCriadoEm(agora);
            novo.registrarConsentimento(agora, mesesDeRetencao);
            return candidatoRepository.save(novo);
        });

        if (existente.isEmpty()) {
            candidato.setPathCurriculo(storageService.save(curriculo, candidato.getId().toString()));
            candidatoRepository.save(candidato);
        }

        enviarLinkSePuder(candidato);
    }

    /**
     * "Quero ver o que enviei."
     *
     * <p><b>Nunca lança, e nunca distingue.</b> Quem chamou responde 202 sempre,
     * exista ou não o e-mail. Um {@code orElseThrow} aqui transformaria a rota
     * num oráculo por 404.
     *
     * <p>Também não manda "não temos cadastro seu": isso viraria um canhão de
     * spam apontado para qualquer endereço do mundo, num projeto sem rate
     * limiting.
     */
    @Transactional
    public void pedirLinkDeAcesso(String email) {
        candidatoRepository.findByEmail_AddressIgnoreCase(normalizar(email))
                .filter(c -> !c.estaAnonimizado())
                .ifPresent(this::enviarLinkSePuder);
    }

    public TalentBankEntryDTO verPorToken(String token) {
        return paraDTO(candidatoDoToken(token));
    }

    @Transactional
    public TalentBankEntryDTO atualizarPorToken(String token,
                                                UpdateTalentBankEntryDTO dto,
                                                MultipartFile curriculo) throws IOException {
        Candidato candidato = candidatoDoToken(token);
        LocalDateTime agora = LocalDateTime.now(clock);

        candidato.setNome(dto.nome());
        candidato.setTelefone(dto.telefone());
        candidato.setUrlLinkedin(dto.urlLinkedin());
        candidato.setAreaInteresse(dto.areaInteresse());
        candidato.setAtualizadoEm(agora);

        if (dto.consentimento()) {
            candidato.registrarConsentimento(agora, mesesDeRetencao);
        }

        if (curriculo != null && !curriculo.isEmpty()) {
            String anterior = candidato.getPathCurriculo();
            String novo = storageService.save(curriculo, candidato.getId().toString());

            // Extensão diferente deixa o arquivo velho no disco para sempre — o
            // nome é `<id>.<ext>`, então `.pdf` e `.docx` são dois arquivos —
            // enquanto a pessoa acredita ter substituído.
            if (anterior != null && !anterior.equals(novo)) {
                storageService.delete(anterior);
            }

            candidato.setPathCurriculo(novo);
        }

        candidatoRepository.save(candidato);
        return paraDTO(candidato);
    }

    /**
     * "Apaguem meus dados."
     *
     * <p>Dois caminhos, e são dois porque a FK obriga: sem candidatura a linha
     * sai inteira; com candidatura ela é anonimizada. Um {@code DELETE} duro
     * estouraria a FK — ou, com cascade, levaria junto {@code candidaturas},
     * {@code historico_etapas} e {@code resposta_perguntas} de uma contratação
     * real.
     */
    @Transactional
    public void excluirPorToken(String token) throws IOException {
        excluirOuAnonimizar(candidatoDoToken(token));
    }

    public Path curriculoPorToken(String token) {
        Candidato candidato = candidatoDoToken(token);

        if (candidato.getPathCurriculo() == null) {
            throw new CandidatoNotFoundException();
        }

        return storageService.searchFile(candidato.getPathCurriculo());
    }

    // ─── Interno ─────────────────────────────────────────────────────────────

    /**
     * A lista da aba, filtrada em memória.
     *
     * <p>São 21 candidatos em produção hoje, e a base cresce em dezenas por
     * ano. <b>Teto conhecido: passar para consulta paginada acima de ~2000
     * linhas</b> — está escrito aqui para ser uma decisão, e não um esquecimento
     * que alguém descobre com a tela travando.
     *
     * @param origem        {@code ESPONTANEO} traz quem nunca se candidatou a
     *                      vaga nenhuma. É um recorte, não a definição de estar
     *                      no banco
     * @param consentimento {@code SEM} traz as linhas anteriores a 2026-09-11,
     *                      que a tela desenha com marcador próprio
     */
    public List<TalentBankSummaryDTO> listar(String termo, String area,
                                             String origem, String consentimento, String situacao) {
        LocalDateTime agora = LocalDateTime.now(clock);
        Map<UUID, Integer> candidaturasPorCandidato = contagemDeCandidaturas();
        String busca = termo == null || termo.isBlank() ? null : termo.trim().toLowerCase(Locale.ROOT);
        String areaBuscada = area == null || area.isBlank() ? null : area.trim().toLowerCase(Locale.ROOT);

        return candidatoRepository.findAllByAnonimizadoEmIsNull().stream()
                .filter(c -> busca == null || casaComABusca(c, busca))
                .filter(c -> areaBuscada == null
                        || (c.getAreaInteresse() != null
                            && c.getAreaInteresse().toLowerCase(Locale.ROOT).contains(areaBuscada)))
                .filter(c -> casaComAOrigem(c, origem, candidaturasPorCandidato))
                .filter(c -> casaComOConsentimento(c, consentimento))
                .filter(c -> casaComASituacao(c, situacao, agora))
                .sorted(Comparator.comparing(TalentBankService::maisRecenteDe,
                        Comparator.nullsLast(Comparator.reverseOrder())))
                .map(c -> paraResumo(c, candidaturasPorCandidato.getOrDefault(c.getId(), 0)))
                .toList();
    }

    public Path curriculoDe(UUID candidatoId) {
        Candidato candidato = candidatoRepository.findById(candidatoId)
                .orElseThrow(CandidatoNotFoundException::new);

        if (candidato.getPathCurriculo() == null) {
            throw new CandidatoNotFoundException();
        }

        return storageService.searchFile(candidato.getPathCurriculo());
    }

    public String nomeDoArquivoDe(UUID candidatoId) {
        return candidatoRepository.findById(candidatoId)
                .map(Candidato::getPathCurriculo)
                .orElseThrow(CandidatoNotFoundException::new);
    }

    /** Exclusão pedida por telefone, feita pelo RH. Mesma rotina do token. */
    @Transactional
    public void excluir(UUID candidatoId) throws IOException {
        excluirOuAnonimizar(candidatoRepository.findById(candidatoId)
                .orElseThrow(CandidatoNotFoundException::new));
    }

    // ─── Bastidores ──────────────────────────────────────────────────────────

    private void enviarLinkSePuder(Candidato candidato) {
        tokenService.emitirPara(candidato).ifPresent(token ->
                emailService.enviarLinkDeAcesso(
                        candidato.getEmail().getAddress(), candidato.getNome(), token));
    }

    /**
     * Resolve o token, separando "não existe" de "não vale mais".
     *
     * <p>Distinguir 404 de 410 aqui não vaza nada: para chegar ao endpoint você
     * já tem um token na mão. E muda a frase que a pessoa lê.
     */
    private Candidato candidatoDoToken(String token) {
        TalentBankAccessToken acesso = tokenService.resolver(token)
                .orElseThrow(LinkDeAcessoInvalidoException::new);

        if (!tokenService.valido(acesso)) {
            throw new LinkDeAcessoExpiradoException();
        }

        Candidato candidato = acesso.getCandidato();

        if (candidato.estaAnonimizado()) {
            throw new LinkDeAcessoExpiradoException();
        }

        return candidato;
    }

    private void excluirOuAnonimizar(Candidato candidato) throws IOException {
        String arquivo = candidato.getPathCurriculo();

        tokenService.apagarTodosDe(candidato);

        if (candidaturaRepository.existsByCandidato(candidato)) {
            candidato.anonimizar(LocalDateTime.now(clock));
            candidatoRepository.save(candidato);
        } else {
            candidatoRepository.delete(candidato);
        }

        // Por último: o disco não participa da transação, então apagar antes
        // deixaria o arquivo perdido se o banco desfizesse a operação.
        if (arquivo != null) {
            storageService.delete(arquivo);
        }
    }

    private Map<UUID, Integer> contagemDeCandidaturas() {
        Map<UUID, Integer> mapa = new HashMap<>();
        for (Object[] linha : candidaturaRepository.contarPorCandidato()) {
            mapa.put((UUID) linha[0], ((Number) linha[1]).intValue());
        }
        return mapa;
    }

    private static boolean casaComABusca(Candidato c, String busca) {
        return contem(c.getNome(), busca)
                || contem(c.getEmail() == null ? null : c.getEmail().getAddress(), busca)
                || contem(c.getTelefone(), busca);
    }

    private static boolean contem(String valor, String busca) {
        return valor != null && valor.toLowerCase(Locale.ROOT).contains(busca);
    }

    private static boolean casaComAOrigem(Candidato c, String origem, Map<UUID, Integer> contagem) {
        if (origem == null || origem.isBlank() || "TODOS".equalsIgnoreCase(origem)) return true;
        boolean espontaneo = contagem.getOrDefault(c.getId(), 0) == 0;
        return "ESPONTANEO".equalsIgnoreCase(origem) == espontaneo;
    }

    private static boolean casaComOConsentimento(Candidato c, String consentimento) {
        if (consentimento == null || consentimento.isBlank() || "TODOS".equalsIgnoreCase(consentimento)) return true;
        boolean tem = c.getConsentimentoEm() != null;
        return "COM".equalsIgnoreCase(consentimento) == tem;
    }

    private static boolean casaComASituacao(Candidato c, String situacao, LocalDateTime agora) {
        if (situacao == null || situacao.isBlank() || "TODOS".equalsIgnoreCase(situacao)) return true;
        boolean vencido = c.getExpiraEm() != null && c.getExpiraEm().isBefore(agora);
        return "VENCIDO".equalsIgnoreCase(situacao) == vencido;
    }

    private static LocalDateTime maisRecenteDe(Candidato c) {
        return c.getAtualizadoEm() != null ? c.getAtualizadoEm() : c.getCriadoEm();
    }

    private TalentBankEntryDTO paraDTO(Candidato c) {
        List<TalentBankEntryDTO.CandidaturaResumoDTO> candidaturas =
                candidaturaRepository.findAllByCandidato(c).stream()
                        .map(k -> new TalentBankEntryDTO.CandidaturaResumoDTO(
                                k.getVaga().getTitulo(), k.getCriadoEm()))
                        .toList();

        return new TalentBankEntryDTO(
                c.getNome(),
                c.getEmail().getAddress(),
                c.getTelefone(),
                c.getUrlLinkedin(),
                c.getAreaInteresse(),
                c.getPathCurriculo() != null,
                StringUtils.getFilenameExtension(c.getPathCurriculo()),
                c.getCriadoEm(),
                c.getAtualizadoEm(),
                c.getConsentimentoEm(),
                c.getExpiraEm(),
                candidaturas);
    }

    private static TalentBankSummaryDTO paraResumo(Candidato c, int quantidadeDeCandidaturas) {
        return new TalentBankSummaryDTO(
                c.getId(),
                c.getNome(),
                c.getEmail().getAddress(),
                c.getTelefone(),
                c.getUrlLinkedin(),
                c.getAreaInteresse(),
                c.getPathCurriculo() != null,
                quantidadeDeCandidaturas == 0,
                quantidadeDeCandidaturas,
                c.getCriadoEm(),
                c.getAtualizadoEm(),
                c.getConsentimentoEm(),
                c.getExpiraEm());
    }

    private static String normalizar(String email) {
        return email == null ? null : email.trim().toLowerCase(Locale.ROOT);
    }
}
