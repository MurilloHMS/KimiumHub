package com.proautokimium.api.domain.valueObjects;

import com.proautokimium.api.domain.exceptions.email.EmailInvalidException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * O value object do e-mail.
 *
 * <p><b>Este arquivo não existia, e é por isso que o defeito durou tanto.</b>
 * Onze arquivos constroem {@code Email} — quinze construções ao todo — e nenhum
 * teste exercitava o construtor. Enquanto ele lançava
 * {@code IllegalArgumentException}, que não está mapeada em lugar nenhum, um
 * e-mail digitado errado chegava ao usuário como <b>500</b>: indistinguível de a
 * API ter caído.
 */
class EmailTest {

    // ── O que passa ───────────────────────────────────────────────────────────

    @Test
    @DisplayName("aceita um endereço comum")
    void aceitaEnderecoComum() {
        assertThat(new Email("contato@presmak.com.br").getAddress())
                .isEqualTo("contato@presmak.com.br");
    }

    @Test
    @DisplayName("aceita ponto, mais e hífen, que são legítimos")
    void aceitaOsSinaisLegitimos() {
        assertThatCode(() -> new Email("joao.silva+rh@proauto-kimium.com.br"))
                .doesNotThrowAnyException();
    }

    // ── O que não passa ───────────────────────────────────────────────────────

    @Test
    @DisplayName("nulo e vazio não viram e-mail")
    void nuloEVazioNaoPassam() {
        assertThatThrownBy(() -> new Email(null)).isInstanceOf(EmailInvalidException.class);
        assertThatThrownBy(() -> new Email("")).isInstanceOf(EmailInvalidException.class);
        assertThatThrownBy(() -> new Email("   ")).isInstanceOf(EmailInvalidException.class);
    }

    @Test
    @DisplayName("endereço pela metade não passa")
    void enderecoPelaMetadeNaoPassa() {
        assertThatThrownBy(() -> new Email("joao@")).isInstanceOf(EmailInvalidException.class);
        assertThatThrownBy(() -> new Email("@empresa.com")).isInstanceOf(EmailInvalidException.class);
        assertThatThrownBy(() -> new Email("joao")).isInstanceOf(EmailInvalidException.class);
    }

    /**
     * O domínio sem ponto é o caso que o {@code @Email} do Jakarta aceitaria.
     * É por isso que a validação daqui não pode ser delegada a ele.
     */
    @Test
    @DisplayName("domínio sem ponto não passa")
    void dominioSemPontoNaoPassa() {
        assertThatThrownBy(() -> new Email("joao@empresa")).isInstanceOf(EmailInvalidException.class);
    }

    /**
     * <b>O caso que mais vai aparecer na conciliação com o Sankhya.</b>
     *
     * <p>Campo de e-mail de ERP guarda dois endereços separados por {@code ;} ou
     * {@code ,} o tempo todo. Aqui ele é recusado — e é a conciliação que vai
     * decidir pegar o primeiro, antes de chegar neste construtor.
     */
    @Test
    @DisplayName("dois endereços no mesmo campo não passam")
    void doisEnderecosNaoPassam() {
        assertThatThrownBy(() -> new Email("a@x.com;b@y.com")).isInstanceOf(EmailInvalidException.class);
        assertThatThrownBy(() -> new Email("a@x.com, b@y.com")).isInstanceOf(EmailInvalidException.class);
    }

    // ── O que o erro diz ──────────────────────────────────────────────────────

    /**
     * <b>400, e não 500.</b> É a razão de a exceção existir: o erro do usuário
     * precisa ser distinguível de um defeito da API.
     */
    @Test
    @DisplayName("o erro é 400, e não erro de servidor")
    void oErroEQuatrocentos() {
        EmailInvalidException erro = catchEmail("joao@");

        assertThat(erro.getStatus())
                .as("500 faria um erro de digitação parecer a API fora do ar")
                .isEqualTo(HttpStatus.BAD_REQUEST);
    }

    /**
     * A mensagem carrega o endereço recusado.
     *
     * <p>Com 900 clientes vindo do ERP, "e-mail inválido" não acha a linha. O
     * valor é o que permite ir consertar no Sankhya.
     */
    @Test
    @DisplayName("a mensagem diz qual endereço foi recusado")
    void aMensagemDizQualEndereco() {
        assertThat(catchEmail("joao@").getMessage()).contains("joao@");
    }

    // ── Igualdade ─────────────────────────────────────────────────────────────

    @Test
    @DisplayName("dois e-mails iguais são o mesmo valor")
    void igualdadePorValor() {
        assertThat(new Email("a@x.com")).isEqualTo(new Email("a@x.com"));
        assertThat(new Email("a@x.com")).isNotEqualTo(new Email("b@x.com"));
        assertThat(new Email("a@x.com").hashCode()).isEqualTo(new Email("a@x.com").hashCode());
    }

    private EmailInvalidException catchEmail(String valor) {
        try {
            new Email(valor);
        } catch (EmailInvalidException e) {
            return e;
        }
        throw new AssertionError("esperava EmailInvalidException para: " + valor);
    }
}
