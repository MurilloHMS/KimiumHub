package com.proautokimium.api.Infrastructure.services.authentication;

import com.proautokimium.api.Infrastructure.repositories.RefreshTokenRepository;
import com.proautokimium.api.domain.entities.auth.RefreshToken;
import com.proautokimium.api.domain.entities.auth.User;
import com.proautokimium.api.domain.exceptions.auth.RefreshTokenInvalidoException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.HexFormat;
import java.util.List;

/**
 * A renovação da sessão.
 *
 * O access token é um JWT de duas horas e continua como está: sem estado, sem
 * consulta, validado só pela assinatura. Este serviço cuida do outro token — o
 * que vive dias e existe para evitar digitar a senha a cada duas horas.
 *
 * <h2>Por que este fica no banco</h2>
 *
 * JWT não se cancela: enquanto a data não vence ele vale, e não há onde dizer o
 * contrário. Para um token de duas horas isso é aceitável. Para um de sete dias
 * não é — sem cancelamento, "Sair" não desliga nada e uma cópia vazada funciona
 * a semana inteira.
 *
 * <h2>O que a tabela guarda</h2>
 *
 * O hash, nunca o token. Quem tiver uma cópia do banco tem o hash, e hash não
 * abre sessão — é a mesma razão pela qual a senha ao lado também não está lá em
 * claro.
 *
 * <h2>Rotação e detecção de reuso</h2>
 *
 * Cada renovação queima o token usado e emite outro. Isso reduz a janela de um
 * vazamento a uma única renovação e, mais importante, torna o vazamento
 * VISÍVEL: um token já usado chegando de novo não é engano de quem digita, é
 * uma segunda cópia em campo. A reação é derrubar todas as sessões da pessoa.
 */
@Service
public class RefreshTokenService {

    private static final Logger log = LoggerFactory.getLogger(RefreshTokenService.class);

    /** Sete dias: uma semana de trabalho sem digitar a senha de novo. */
    private static final int DIAS_DE_VIDA = 7;

    /** 32 bytes de aleatoriedade — 256 bits, o mesmo tamanho do hash. */
    private static final int BYTES_DO_TOKEN = 32;

    private final RefreshTokenRepository tokens;
    private final Clock clock;
    private final SecureRandom random = new SecureRandom();

    public RefreshTokenService(RefreshTokenRepository tokens, Clock clock) {
        this.tokens = tokens;
        this.clock = clock;
    }

    /**
     * Cria um token novo para a pessoa e devolve o valor CRU.
     *
     * É a única vez que o valor cru existe fora do navegador dela. O que fica
     * gravado é o hash, e não há como voltar dele para o token — se a pessoa
     * perder o valor, o caminho é entrar de novo.
     */
    @Transactional
    public String emitir(User user) {
        return emitirPara(user.getId(), LocalDateTime.now(clock));
    }

    /**
     * Troca um token válido por outro, e devolve de quem ele era.
     *
     * Quem chama emite um access token novo para o usuário devolvido. A troca é
     * atômica: o antigo é queimado e o novo nasce na mesma transação, então não
     * existe instante em que os dois valham.
     *
     * @throws RefreshTokenInvalidoException para qualquer motivo de recusa —
     *         não existe, venceu, já foi usado ou foi revogado.
     */
    @Transactional
    public Renovacao renovar(String cru) {
        RefreshToken token = tokens.findByTokenHash(hash(cru))
                .orElseThrow(RefreshTokenInvalidoException::new);

        LocalDateTime agora = LocalDateTime.now(clock);

        // Token já usado chegando de novo é a assinatura de um vazamento: quem
        // é dono da sessão recebeu o substituto e não voltaria com este. Como
        // não dá para saber qual das duas cópias é a da pessoa certa, as duas
        // caem — melhor incomodar uma pessoa do que manter duas sessões vivas
        // com a mesma credencial.
        if (token.getUsedAt() != null) {
            log.warn("Refresh token reutilizado — revogando as sessões do usuário {}", token.getUserId());
            revogarTudo(token.getUserId());
            throw new RefreshTokenInvalidoException();
        }

        if (!token.isValid(agora)) {
            throw new RefreshTokenInvalidoException();
        }

        token.markUsed(agora);
        tokens.save(token);

        return new Renovacao(token.getUserId(), emitirPara(token.getUserId(), agora));
    }

    /**
     * Derruba todas as sessões vivas de uma pessoa.
     *
     * Chamado no logout e na detecção de reuso. Idempotente: quem não tem token
     * vivo simplesmente não tem o que revogar.
     */
    @Transactional
    public int revogarTudo(String userId) {
        List<RefreshToken> vivos = tokens.findByUserIdAndUsedAtIsNullAndRevokedAtIsNull(userId);
        LocalDateTime agora = LocalDateTime.now(clock);

        vivos.forEach(token -> token.setRevokedAt(agora));
        tokens.saveAll(vivos);
        return vivos.size();
    }

    /**
     * Encerra a sessão a partir do refresh token que o navegador tem.
     *
     * <p><b>Nunca reclama.</b> Token desconhecido, vencido ou já revogado sai
     * pelo mesmo caminho silencioso — quem apertou "Sair" quer sair, e um erro
     * ali só serviria para deixar a pessoa presa numa tela que ela está tentando
     * abandonar. Idempotente por consequência: apertar duas vezes é inofensivo.
     *
     * <p>Revoga TODAS as sessões vivas da pessoa, e não só esta. Quem sai
     * costuma querer sair de tudo, e o caso que importa é o computador
     * emprestado: sair no celular precisa fechar a sessão que ficou aberta lá.
     *
     * @return quantas sessões caíram — zero quando não havia o que encerrar
     */
    @Transactional
    public int encerrar(String cru) {
        if (cru == null || cru.isBlank()) return 0;

        return tokens.findByTokenHash(hash(cru))
                .map(token -> revogarTudo(token.getUserId()))
                .orElse(0);
    }

    /** O par que a renovação devolve: de quem era, e o token que substitui. */
    public record Renovacao(String userId, String refreshToken) { }

    // ─── Interno ─────────────────────────────────────────────────────────────

    private String emitirPara(String userId, LocalDateTime agora) {
        byte[] bytes = new byte[BYTES_DO_TOKEN];
        random.nextBytes(bytes);
        String cru = Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);

        RefreshToken novo = new RefreshToken();
        novo.setUserId(userId);
        novo.setTokenHash(hash(cru));
        novo.setExpiresAt(agora.plusDays(DIAS_DE_VIDA));
        novo.setCreatedAt(agora);

        tokens.save(novo);
        return cru;
    }

    /**
     * SHA-256, e não BCrypt.
     *
     * BCrypt é lento de propósito, para encarecer o chute de uma senha humana.
     * Aqui não há o que chutar: são 256 bits de aleatoriedade. A lentidão não
     * compraria segurança nenhuma e seria paga em toda renovação.
     */
    private String hash(String cru) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(digest.digest(cru.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException e) {
            // SHA-256 é obrigatório em toda JVM. Faltando, o ambiente está
            // quebrado de um jeito que não é assunto deste método.
            throw new IllegalStateException("SHA-256 indisponível nesta JVM", e);
        }
    }
}
