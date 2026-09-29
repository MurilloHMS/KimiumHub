package com.proautokimium.api.Infrastructure.services.storage;

import com.proautokimium.api.domain.exceptions.humanResources.InvalidRequestDataException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.test.util.ReflectionTestUtils;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * **O nome do arquivo não escolhe onde ele é gravado.**
 *
 * Antes, o nome original ia cru para o caminho: "../../x.pdf" escrevia fora da
 * pasta do funcionário (achado da auditoria de 2026-09-28). Disco de verdade
 * numa pasta temporária — é o caminho real que importa, não um mock dele.
 */
class EmployeeDocumentStorageServiceTest {

    @TempDir Path root;

    private EmployeeDocumentStorageService storage;

    @BeforeEach
    void setUp() {
        storage = new EmployeeDocumentStorageService();
        ReflectionTestUtils.setField(storage, "storagePath", root.toString());
    }

    @Test
    @DisplayName("nome com ../ grava dentro da pasta do funcionário")
    void travessiaFicaDentro() throws Exception {
        String relative = storage.save(new byte[]{1}, "EMP001", "../../../fora.pdf");

        Path written = storage.resolve(relative);
        assertThat(written.startsWith(root.resolve("EMP001"))).isTrue();
        assertThat(Files.exists(written)).isTrue();
        assertThat(relative).endsWith("-fora.pdf");
    }

    @Test
    @DisplayName("o navegador que manda o caminho inteiro fica só com o nome")
    void soOUltimoPedaco() {
        assertThat(EmployeeDocumentStorageService.safeName("C:\\Users\\ana\\ASO 2026.pdf", "x"))
                .doesNotContain("\\").endsWith(".pdf").startsWith("ASO");
        assertThat(EmployeeDocumentStorageService.safeName("/etc/passwd", "x")).isEqualTo("passwd");
    }

    @Test
    @DisplayName("nome que sobra vazio vira o padrão")
    void nomeVazio() {
        assertThat(EmployeeDocumentStorageService.safeName("..", "documento")).isEqualTo("documento");
        assertThat(EmployeeDocumentStorageService.safeName(null, "documento")).isEqualTo("documento");
    }

    /** O caminho que vem do banco também é conferido: `resolve` é a outra porta. */
    @Test
    @DisplayName("resolver um caminho que sai da raiz é recusado")
    void resolveForaDaRaiz() {
        assertThatThrownBy(() -> storage.resolve("../../etc/passwd"))
                .isInstanceOf(InvalidRequestDataException.class);
    }

    @Test
    @DisplayName("apagar remove o arquivo, e apagar de novo não quebra")
    void apaga() throws Exception {
        String relative = storage.save(new byte[]{1}, "EMP001", "a.pdf");

        storage.delete(relative);
        storage.delete(relative);

        assertThat(Files.exists(storage.resolve(relative))).isFalse();
    }
}
