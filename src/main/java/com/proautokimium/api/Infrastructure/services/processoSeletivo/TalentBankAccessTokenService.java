package com.proautokimium.api.Infrastructure.services.processoSeletivo;

import com.proautokimium.api.Infrastructure.repositories.processoSeletivo.TalentBankAccessTokenRepository;
import com.proautokimium.api.Infrastructure.services.secrets.CryptoTokenService;
import com.proautokimium.api.domain.entities.processoSeletivo.Candidato;
import com.proautokimium.api.domain.entities.processoSeletivo.TalentBankAccessToken;
import jakarta.transaction.Transactional;
import org.springframework.stereotype.Service;

import java.security.NoSuchAlgorithmException;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.Optional;

/**
 * Emite e resolve os links de acesso do candidato ao próprio cadastro.
 */
@Service
public class TalentBankAccessTokenService {

    /**
     * 24 horas, e o e-mail lê desta constante — nunca do próprio texto.
     *
     * <p>Não são os 30 min do primeiro acesso, que é código de 6 caracteres
     * digitado à mão com a tela aberta. Não são as 48h do convite de cliente,
     * que é não solicitado e pode esperar o expediente seguinte. Este é
     * solicitado — a pessoa acabou de pedir e está no e-mail —, mas a tarefa do
     * outro lado é "achar meu currículo e subir", que é tarefa de notebook.
     */
    public static final int TOKEN_TTL_HORAS = 24;

    /**
     * O mesmo cooldown que a tela de primeiro acesso já mostra.
     *
     * <p>É o que fecha a amplificação de e-mail sem biblioteca de rate limiting:
     * sem ele, uma rota pública dispara quantas mensagens alguém quiser para um
     * endereço que esteja na base.
     */
    public static final int COOLDOWN_SEGUNDOS = 60;

    private final TalentBankAccessTokenRepository repository;
    private final CryptoTokenService cryptoTokenService;
    private final Clock clock;

    public TalentBankAccessTokenService(TalentBankAccessTokenRepository repository,
                                        CryptoTokenService cryptoTokenService,
                                        Clock clock) {
        this.repository = repository;
        this.cryptoTokenService = cryptoTokenService;
        this.clock = clock;
    }

    /**
     * Emite um link novo e mata os anteriores.
     *
     * @return o token <b>em claro</b>, que só existe aqui e no e-mail — o banco
     *         guarda apenas o hash. Vazio quando o cooldown está valendo: nesse
     *         caso não há token novo e não há e-mail, e quem chamou responde
     *         exatamente a mesma coisa de sempre.
     */
    @Transactional
    public Optional<String> emitirPara(Candidato candidato) {
        LocalDateTime agora = LocalDateTime.now(clock);

        if (repository.existsByCandidatoAndRevokedAtIsNullAndCreatedAtAfter(
                candidato, agora.minusSeconds(COOLDOWN_SEGUNDOS))) {
            return Optional.empty();
        }

        repository.revogarVivosDo(candidato, agora);

        String token = cryptoTokenService.generateToken();

        TalentBankAccessToken novo = new TalentBankAccessToken();
        novo.setTokenHash(hash(token));
        novo.setCandidato(candidato);
        novo.setCreatedAt(agora);
        novo.setExpiresAt(agora.plusHours(TOKEN_TTL_HORAS));

        repository.save(novo);

        return Optional.of(token);
    }

    /**
     * Resolve um token em claro.
     *
     * <p>Devolve vazio tanto para desconhecido quanto para expirado ou
     * revogado: quem distingue os dois é o chamador, que tem o contexto para
     * escolher entre 404 e 410.
     */
    public Optional<TalentBankAccessToken> resolver(String token) {
        if (token == null || token.isBlank()) {
            return Optional.empty();
        }

        return repository.findByTokenHash(hash(token));
    }

    public boolean valido(TalentBankAccessToken token) {
        return token.isValid(LocalDateTime.now(clock));
    }

    @Transactional
    public void revogarTodosDe(Candidato candidato) {
        repository.revogarVivosDo(candidato, LocalDateTime.now(clock));
    }

    /**
     * Apaga as linhas de token do candidato.
     *
     * <p>Usado na exclusão de dados, e é {@code delete} e não {@code revoke}
     * por dois motivos: a FK para {@code candidatos} impediria o
     * {@code DELETE} da pessoa, e um link vivo apontando para um cadastro
     * anonimizado é um defeito esperando acontecer.
     */
    @Transactional
    public void apagarTodosDe(Candidato candidato) {
        repository.deleteAll(repository.findAllByCandidato(candidato));
    }

    /**
     * SHA-256, do {@code CryptoTokenService} que já existe.
     *
     * <p>A {@code NoSuchAlgorithmException} é embrulhada porque SHA-256 sempre
     * existe numa JVM: se faltar, não é erro de negócio nem coisa que quem
     * chamou possa tratar.
     */
    private String hash(String token) {
        try {
            return cryptoTokenService.hashToken(token);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 indisponível na JVM", e);
        }
    }
}
