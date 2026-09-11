package com.proautokimium.api.domain.entities.processoSeletivo;

import com.proautokimium.api.domain.abstractions.Entity;
import com.proautokimium.api.domain.valueObjects.Email;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.time.LocalDateTime;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Consentimento e anonimização, as duas regras do banco de talentos que moram
 * na entidade.
 */
class CandidatoConsentimentoTest {

    private static final LocalDateTime AGORA = LocalDateTime.of(2026, 9, 11, 14, 0);
    private static final int RETENCAO = 24;

    private static Candidato candidato() throws Exception {
        Candidato c = new Candidato();
        c.setNome("Maria Souza");
        c.setEmail(new Email("maria@email.com"));
        c.setTelefone("44999990000");
        c.setUrlLinkedin("linkedin.com/in/maria");
        c.setPathCurriculo("abc.pdf");
        c.setCriadoEm(AGORA.minusYears(1));

        Field id = Entity.class.getDeclaredField("id");
        id.setAccessible(true);
        id.set(c, UUID.randomUUID());

        return c;
    }

    /**
     * A expiração é gravada como <b>data resolvida</b>. Calculada na leitura a
     * partir de {@code criadoEm}, uma renovação não renovaria nada — e mudar a
     * retenção de 24 para 12 meses expiraria metade do banco numa madrugada.
     */
    @Test
    @DisplayName("Consentimento grava a data e resolve a expiracao")
    void gravaDataEExpiracao() throws Exception {
        Candidato c = candidato();

        c.registrarConsentimento(AGORA, RETENCAO);

        assertThat(c.getConsentimentoEm()).isEqualTo(AGORA);
        assertThat(c.getExpiraEm())
                .as("a expiracao sai de AGORA, nunca de criadoEm")
                .isEqualTo(AGORA.plusMonths(RETENCAO));
    }

    /**
     * Renovar é chamar de novo. Uma guarda de "só se estiver nulo" tornaria a
     * expiração irrenovável, e o agendador apagaria gente que acabou de
     * confirmar que quer ficar.
     */
    @Test
    @DisplayName("Registrar de novo empurra as duas datas")
    void renovarEmpurraAsDatas() throws Exception {
        Candidato c = candidato();
        c.registrarConsentimento(AGORA.minusMonths(20), RETENCAO);

        c.registrarConsentimento(AGORA, RETENCAO);

        assertThat(c.getConsentimentoEm()).isEqualTo(AGORA);
        assertThat(c.getExpiraEm()).isEqualTo(AGORA.plusMonths(RETENCAO));
    }

    @Test
    @DisplayName("Consentimento vencido nao vale")
    void vencidoNaoVale() throws Exception {
        Candidato c = candidato();
        c.registrarConsentimento(AGORA.minusMonths(25), RETENCAO);

        assertThat(c.consentimentoValidoEm(AGORA)).isFalse();
    }

    /**
     * Zerar a expiração junto é o que impede o agendador de "expirar" para
     * sempre uma linha que já está anônima.
     */
    @Test
    @DisplayName("Anonimizar limpa os dados pessoais e zera consentimento E expiracao")
    void anonimizarLimpaTudo() throws Exception {
        Candidato c = candidato();
        c.registrarConsentimento(AGORA.minusMonths(1), RETENCAO);

        c.anonimizar(AGORA);

        assertThat(c.getNome()).isEqualTo("Candidato removido");
        assertThat(c.getTelefone()).isEmpty();
        assertThat(c.getUrlLinkedin()).isNull();
        assertThat(c.getPathCurriculo()).isNull();
        assertThat(c.getConsentimentoEm()).isNull();
        assertThat(c.getExpiraEm())
                .as("expiracao sobrevivente faria o agendador voltar nesta linha para sempre")
                .isNull();
        assertThat(c.getAnonimizadoEm()).isEqualTo(AGORA);
        assertThat(c.estaAnonimizado()).isTrue();
    }

    /**
     * <b>A lápide precisa ser única por id.</b> Um endereço fixo violaria o
     * índice único de e-mail na segunda exclusão — 500 num endpoint público,
     * para alguém que só pediu para ser esquecido.
     *
     * <p>E o domínio é {@code .invalid}, reservado pela RFC 2606: nenhum e-mail
     * nosso escapa para um destino real.
     */
    @Test
    @DisplayName("Duas anonimizacoes geram lapides diferentes")
    void lapidesNaoColidem() throws Exception {
        Candidato primeiro = candidato();
        Candidato segundo = candidato();

        primeiro.anonimizar(AGORA);
        segundo.anonimizar(AGORA);

        assertThat(primeiro.getEmail().getAddress()).endsWith("@removido.invalid");
        assertThat(primeiro.getEmail().getAddress())
                .as("lapide fixa colide no indice unico na segunda exclusao")
                .isNotEqualTo(segundo.getEmail().getAddress());
    }
}
