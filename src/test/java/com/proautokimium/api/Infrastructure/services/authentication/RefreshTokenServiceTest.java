package com.proautokimium.api.Infrastructure.services.authentication;

import com.proautokimium.api.Infrastructure.repositories.RefreshTokenRepository;
import com.proautokimium.api.domain.entities.auth.RefreshToken;
import com.proautokimium.api.domain.entities.auth.User;
import com.proautokimium.api.domain.exceptions.auth.RefreshTokenInvalidoException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * A renovação da sessão.
 *
 * O que estes testes protegem é o que separa "não digitar a senha a cada duas
 * horas" de "uma credencial de sete dias que ninguém consegue cancelar". Os
 * erros aqui não aparecem em tela: um token que continua valendo depois do
 * logout funciona perfeitamente — para quem o roubou.
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class RefreshTokenServiceTest {

    private static final String USER_ID = "u-1";

    private static final Clock RELOGIO =
            Clock.fixed(Instant.parse("2026-09-01T12:00:00Z"), ZoneId.of("America/Sao_Paulo"));

    private static final LocalDateTime AGORA = LocalDateTime.now(RELOGIO);

    @Mock private RefreshTokenRepository tokens;

    private RefreshTokenService service;

    @BeforeEach
    void setUp() {
        service = new RefreshTokenService(tokens, RELOGIO);
        when(tokens.save(any())).thenAnswer(i -> i.getArgument(0));
    }

    private static User usuario() {
        User user = new User();
        user.setId(USER_ID);
        return user;
    }

    /** Captura o que foi gravado, na ordem em que foi. */
    private List<RefreshToken> gravados(int quantos) {
        ArgumentCaptor<RefreshToken> captor = ArgumentCaptor.forClass(RefreshToken.class);
        verify(tokens, times(quantos)).save(captor.capture());
        return captor.getAllValues();
    }

    // ─── Emissão ──────────────────────────────────────────────────────────────

    /**
     * **O valor cru nunca é gravado.**
     *
     * É a decisão central desta tabela: quem tiver uma cópia do banco tem o
     * hash, e hash não abre sessão. Guardar o token cru transformaria um dump em
     * acesso a todas as contas — e nada na tela denunciaria isso.
     */
    @Test
    @DisplayName("Grava o hash, nunca o token")
    void gravaOHashNaoOToken() {
        String cru = service.emitir(usuario());

        RefreshToken gravado = gravados(1).get(0);
        assertThat(gravado.getTokenHash()).isNotEqualTo(cru);
        assertThat(gravado.getTokenHash()).hasSize(64);   // SHA-256 em hexadecimal
        assertThat(gravado.getUserId()).isEqualTo(USER_ID);
    }

    /** Sete dias, contados do relógio da aplicação e não do banco. */
    @Test
    @DisplayName("Nasce válido por sete dias")
    void nasceComSeteDias() {
        service.emitir(usuario());

        RefreshToken gravado = gravados(1).get(0);
        assertThat(gravado.getExpiresAt()).isEqualTo(AGORA.plusDays(7));
        assertThat(gravado.isValid(AGORA)).isTrue();
        assertThat(gravado.isValid(AGORA.plusDays(8))).isFalse();
    }

    /**
     * Dois tokens seguidos não podem coincidir. Parece óbvio, e deixa de ser no
     * dia em que alguém trocar `SecureRandom` por `Random` "porque é mais
     * simples" — aí os valores passam a ser previsíveis a partir do relógio.
     */
    @Test
    @DisplayName("Cada emissão é um valor diferente")
    void cadaEmissaoEhUnica() {
        assertThat(service.emitir(usuario())).isNotEqualTo(service.emitir(usuario()));
    }

    // ─── Renovação ────────────────────────────────────────────────────────────

    /**
     * O par que o teste precisa carregar: a linha gravada e o valor cru.
     *
     * O serviço devolve o cru e guarda o hash, e nunca os dois juntos — que é o
     * ponto. O teste segura as duas pontas porque precisa fingir ser o navegador
     * (que tem o cru) e o banco (que tem o hash).
     */
    private record Emitido(RefreshToken entidade, String cru) { }

    /** Prepara o repositório para reconhecer um token cru já emitido. */
    private Emitido emitidoEReconhecido() {
        String cru = service.emitir(usuario());
        RefreshToken gravado = gravados(1).get(0);

        reset(tokens);
        when(tokens.save(any())).thenAnswer(i -> i.getArgument(0));
        when(tokens.findByTokenHash(gravado.getTokenHash())).thenReturn(Optional.of(gravado));

        return new Emitido(gravado, cru);
    }

    @Test
    @DisplayName("Renovar queima o antigo e devolve um novo")
    void renovarQueimaEEmite() {
        Emitido antigo = emitidoEReconhecido();

        RefreshTokenService.Renovacao renovacao = service.renovar(antigo.cru());

        assertThat(antigo.entidade().getUsedAt()).isEqualTo(AGORA);
        assertThat(renovacao.userId()).isEqualTo(USER_ID);
        assertThat(renovacao.refreshToken()).isNotEqualTo(antigo.cru());
    }

    /**
     * **A detecção de reuso, que é o que a rotação compra.**
     *
     * Um token já usado voltando não é engano de quem digita: quem é dono da
     * sessão recebeu o substituto e não voltaria com este. É a assinatura de uma
     * segunda cópia em campo, e a reação é derrubar tudo — não dá para saber
     * qual das duas é a pessoa certa.
     */
    @Test
    @DisplayName("Token reutilizado derruba todas as sessões da pessoa")
    void reusoDerrubaTudo() {
        Emitido antigo = emitidoEReconhecido();
        antigo.entidade().markUsed(AGORA.minusMinutes(5));

        RefreshToken outro = new RefreshToken();
        outro.setUserId(USER_ID);
        when(tokens.findByUserIdAndUsedAtIsNullAndRevokedAtIsNull(USER_ID)).thenReturn(List.of(outro));

        assertThatThrownBy(() -> service.renovar(antigo.cru()))
                .isInstanceOf(RefreshTokenInvalidoException.class);

        assertThat(outro.getRevokedAt()).isEqualTo(AGORA);
    }

    @Test
    @DisplayName("Token vencido não renova")
    void vencidoNaoRenova() {
        Emitido antigo = emitidoEReconhecido();
        antigo.entidade().setExpiresAt(AGORA.minusDays(1));

        assertThatThrownBy(() -> service.renovar(antigo.cru()))
                .isInstanceOf(RefreshTokenInvalidoException.class);
    }

    /**
     * **O teste que faz o logout significar alguma coisa.**
     *
     * Sem ele, revogar seria uma escrita que ninguém lê: o token continuaria
     * renovando, e "Sair" só apagaria o access token do navegador enquanto a
     * sessão seguia viva do lado do servidor por mais uma semana.
     */
    @Test
    @DisplayName("Token revogado não renova")
    void revogadoNaoRenova() {
        Emitido antigo = emitidoEReconhecido();
        antigo.entidade().setRevokedAt(AGORA.minusHours(1));

        assertThatThrownBy(() -> service.renovar(antigo.cru()))
                .isInstanceOf(RefreshTokenInvalidoException.class);
    }

    @Test
    @DisplayName("Token desconhecido não renova")
    void desconhecidoNaoRenova() {
        when(tokens.findByTokenHash(any())).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.renovar("inventado"))
                .isInstanceOf(RefreshTokenInvalidoException.class);
    }

    // ─── Revogação ────────────────────────────────────────────────────────────

    @Test
    @DisplayName("Revogar marca todos os vivos e conta quantos")
    void revogarMarcaTodos() {
        RefreshToken a = new RefreshToken();
        RefreshToken b = new RefreshToken();
        when(tokens.findByUserIdAndUsedAtIsNullAndRevokedAtIsNull(USER_ID)).thenReturn(List.of(a, b));

        assertThat(service.revogarTudo(USER_ID)).isEqualTo(2);
        assertThat(a.getRevokedAt()).isEqualTo(AGORA);
        assertThat(b.getRevokedAt()).isEqualTo(AGORA);
    }

    /** Quem não tem sessão viva não tem o que revogar, e isso não é erro. */
    @Test
    @DisplayName("Revogar sem sessão viva não estoura")
    void revogarSemSessaoNaoEstoura() {
        when(tokens.findByUserIdAndUsedAtIsNullAndRevokedAtIsNull(USER_ID)).thenReturn(List.of());

        assertThat(service.revogarTudo(USER_ID)).isZero();
    }

    // ─── Encerrar sessão ──────────────────────────────────────────────────────

    /**
     * **O teste que faz "Sair" significar alguma coisa.**
     *
     * Sem isto, sair apagava o que estava no navegador e o refresh continuava
     * válido por sete dias — em outro navegador, ou nas mãos de quem tivesse uma
     * cópia. Apagar o que está na máquina não é encerrar sessão.
     */
    @Test
    @DisplayName("Encerrar revoga todas as sessões da pessoa")
    void encerrarRevogaTudo() {
        Emitido emitido = emitidoEReconhecido();

        RefreshToken outroDispositivo = new RefreshToken();
        outroDispositivo.setUserId(USER_ID);
        when(tokens.findByUserIdAndUsedAtIsNullAndRevokedAtIsNull(USER_ID))
                .thenReturn(List.of(emitido.entidade(), outroDispositivo));

        assertThat(service.encerrar(emitido.cru())).isEqualTo(2);

        // O celular também cai: quem sai costuma querer sair de tudo, e o caso
        // que importa é o computador emprestado.
        assertThat(outroDispositivo.getRevokedAt()).isEqualTo(AGORA);
    }

    /**
     * Sair nunca reclama. Token desconhecido, nulo ou em branco sai pelo mesmo
     * caminho silencioso — quem apertou "Sair" quer sair, e um erro ali só
     * deixaria a pessoa presa na tela que está tentando abandonar.
     */
    @Test
    @DisplayName("Encerrar com token desconhecido ou vazio não estoura")
    void encerrarNuncaEstoura() {
        when(tokens.findByTokenHash(any())).thenReturn(Optional.empty());

        assertThat(service.encerrar("inventado")).isZero();
        assertThat(service.encerrar(null)).isZero();
        assertThat(service.encerrar("  ")).isZero();
    }
}
