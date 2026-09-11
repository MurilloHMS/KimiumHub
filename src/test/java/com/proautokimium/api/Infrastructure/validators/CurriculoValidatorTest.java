package com.proautokimium.api.Infrastructure.validators;

import com.proautokimium.api.Infrastructure.exceptions.processoSeletivo.CurriculoInvalidoException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.multipart.MultipartFile;

import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * A regra sobre o arquivo de currículo.
 *
 * <p>Até 2026-09-11 o envio público não validava <b>nada</b>: qualquer arquivo,
 * de qualquer tipo, até 100 MB, numa rota anônima.
 */
class CurriculoValidatorTest {

    private final CurriculoValidator validator = new CurriculoValidator();

    private static MultipartFile arquivo(String nome, byte[] conteudo) {
        return new MockMultipartFile("curriculo", nome, null, conteudo);
    }

    private static MultipartFile arquivo(String nome, String conteudo) {
        return arquivo(nome, conteudo.getBytes(StandardCharsets.UTF_8));
    }

    /**
     * A linha de base, e ela não é formalidade: uma allow-list que recusasse
     * tudo passaria em todos os testes negativos abaixo.
     */
    @Test
    @DisplayName("PDF de verdade passa")
    void pdfDeVerdadePassa() {
        assertThatCode(() -> validator.validar(arquivo("curriculo.pdf", "%PDF-1.7 conteudo")))
                .doesNotThrowAnyException();
    }

    @Test
    @DisplayName("Extensao fora da lista e recusada")
    void extensaoForaDaListaERecusada() {
        assertThatThrownBy(() -> validator.validar(arquivo("curriculo.exe", "%PDF-1.7")))
                .isInstanceOf(CurriculoInvalidoException.class);
    }

    /**
     * <b>O motivo de conferir os bytes num arquivo que nunca executamos.</b>
     *
     * <p>Nós o servimos de volta. Um HTML renomeado para {@code .pdf}, entregue
     * a partir da origem da API, é XSS armazenado contra a sessão de quem no RH
     * clicar. Extensão é o que o cliente diz; os bytes são o que o arquivo é.
     */
    @Test
    @DisplayName("HTML renomeado para .pdf e recusado pelos bytes")
    void htmlRenomeadoERecusado() {
        assertThatThrownBy(() -> validator.validar(arquivo("curriculo.pdf", "<html><script>")))
                .isInstanceOf(CurriculoInvalidoException.class)
                .hasMessageContaining("PDF de verdade");
    }

    /**
     * O {@code buildFileName} do {@code StorageService} sobrescreve o fallback
     * {@code "bin"} da classe base, então sem esta recusa um arquivo sem
     * extensão vira literalmente {@code <id>.null} no disco.
     */
    @Test
    @DisplayName("Arquivo sem extensao e recusado")
    void semExtensaoERecusado() {
        assertThatThrownBy(() -> validator.validar(arquivo("curriculo", "%PDF-1.7")))
                .isInstanceOf(CurriculoInvalidoException.class);
    }

    @Test
    @DisplayName("Arquivo vazio e recusado")
    void vazioERecusado() {
        assertThatThrownBy(() -> validator.validar(arquivo("curriculo.pdf", new byte[0])))
                .isInstanceOf(CurriculoInvalidoException.class);
    }

    @Test
    @DisplayName("Nulo e recusado, e nao estoura NullPointer")
    void nuloERecusado() {
        assertThatThrownBy(() -> validator.validar(null))
                .isInstanceOf(CurriculoInvalidoException.class);
    }

    /**
     * Um byte acima do teto — o tamanho exato passa, e é isso que separa
     * {@code >} de {@code >=}.
     */
    @Test
    @DisplayName("Um byte acima do teto e recusado com 413")
    void acimaDoTetoERecusadoCom413() {
        byte[] grande = new byte[(int) CurriculoValidator.TAMANHO_MAXIMO_BYTES + 1];
        grande[0] = '%'; grande[1] = 'P'; grande[2] = 'D'; grande[3] = 'F';

        assertThatThrownBy(() -> validator.validar(arquivo("curriculo.pdf", grande)))
                .isInstanceOf(CurriculoInvalidoException.class)
                .satisfies(e -> assertThat(((CurriculoInvalidoException) e).getStatus())
                        .as("arquivo grande demais e 413, nao 400: o arquivo esta certo, so nao cabe")
                        .isEqualTo(HttpStatus.PAYLOAD_TOO_LARGE));
    }

    @Test
    @DisplayName("Exatamente no teto passa")
    void noTetoPassa() {
        byte[] noLimite = new byte[(int) CurriculoValidator.TAMANHO_MAXIMO_BYTES];
        noLimite[0] = '%'; noLimite[1] = 'P'; noLimite[2] = 'D'; noLimite[3] = 'F';

        assertThatCode(() -> validator.validar(arquivo("curriculo.pdf", noLimite)))
                .doesNotThrowAnyException();
    }

    /** Extensão é comparada em minúsculas: `.PDF` do Windows é o caso comum. */
    @Test
    @DisplayName("Extensao em maiuscula passa")
    void extensaoEmMaiusculaPassa() {
        assertThatCode(() -> validator.validar(arquivo("CURRICULO.PDF", "%PDF-1.7")))
                .doesNotThrowAnyException();
    }

    /** Arquivo menor que a assinatura não pode estourar na leitura. */
    @Test
    @DisplayName("Arquivo menor que a assinatura e recusado sem estourar")
    void menorQueAAssinatura() {
        assertThatThrownBy(() -> validator.validar(arquivo("curriculo.pdf", "%P")))
                .isInstanceOf(CurriculoInvalidoException.class);
    }
}
