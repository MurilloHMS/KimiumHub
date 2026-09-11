package com.proautokimium.api.Infrastructure.services.storage;

import com.proautokimium.api.Infrastructure.exceptions.processoSeletivo.CurriculoInvalidoException;
import com.proautokimium.api.Infrastructure.validators.CurriculoValidator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.multipart.MultipartFile;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * O armazenamento dos currículos, e o ponto de estrangulamento da validação.
 *
 * <p><b>O modo de falhar que este arquivo existe para pegar:</b> a regra existe,
 * o caminho não a usa. Um validador injetado e nunca chamado deixa toda a suíte
 * do {@link CurriculoValidator} verde enquanto o upload continua aceitando
 * qualquer coisa — e ninguém descobre, porque os testes do validador testam o
 * validador, não o caminho.
 */
class StorageServiceTest {

    @TempDir Path pasta;

    private StorageService storageService;

    @BeforeEach
    void setUp() {
        storageService = new StorageService(new CurriculoValidator());
        ReflectionTestUtils.setField(storageService, "storagePath", pasta.toString());
    }

    private static MultipartFile arquivo(String nome, String conteudo) {
        return new MockMultipartFile("curriculo", nome, null, conteudo.getBytes(StandardCharsets.UTF_8));
    }

    @Test
    @DisplayName("Arquivo invalido e recusado ANTES de tocar no disco")
    void arquivoInvalidoNaoChegaAoDisco() throws Exception {
        MultipartFile disfarcado = arquivo("curriculo.pdf", "<html><script>alert(1)</script>");

        assertThatThrownBy(() -> storageService.save(disfarcado, UUID.randomUUID().toString()))
                .isInstanceOf(CurriculoInvalidoException.class);

        try (var conteudo = Files.list(pasta)) {
            assertThat(conteudo.toList())
                    .as("recusar depois de gravar deixa o arquivo la, e o 400 vira mentira")
                    .isEmpty();
        }
    }

    @Test
    @DisplayName("PDF valido e gravado como <prefixo>.pdf")
    void pdfValidoEGravado() throws Exception {
        String prefixo = UUID.randomUUID().toString();

        String nome = storageService.save(arquivo("meu curriculo.pdf", "%PDF-1.7 ok"), prefixo);

        assertThat(nome)
                .as("um arquivo por candidato: e o que faz reenviar substituir em vez de acumular")
                .isEqualTo(prefixo + ".pdf");
        assertThat(Files.exists(pasta.resolve(nome))).isTrue();
    }

    /**
     * Substituir é o comportamento desejado — e a consequência, que vale estar
     * escrita, é que <b>não existe histórico de versões</b> do currículo.
     */
    @Test
    @DisplayName("Reenviar substitui o arquivo anterior")
    void reenviarSubstitui() throws Exception {
        String prefixo = UUID.randomUUID().toString();

        storageService.save(arquivo("v1.pdf", "%PDF-1.7 primeiro"), prefixo);
        storageService.save(arquivo("v2.pdf", "%PDF-1.7 segundo"), prefixo);

        try (var conteudo = Files.list(pasta)) {
            assertThat(conteudo.toList()).hasSize(1);
        }
        assertThat(Files.readString(pasta.resolve(prefixo + ".pdf"))).contains("segundo");
    }
}
