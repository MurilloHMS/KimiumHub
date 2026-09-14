package com.proautokimium.api.Infrastructure.services.storage;

import com.proautokimium.api.Infrastructure.exceptions.events.EventExceptions.InvalidEventDataException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.util.ReflectionTestUtils;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * A pasta das capas é pública. Um {@code .png} que é HTML, gravado ali, vira
 * página no domínio da API — por isso a conferência é pelos bytes.
 */
class EventImageStorageServiceTest {

    private static final byte[] PNG = {(byte) 0x89, 'P', 'N', 'G', 0x0D, 0x0A, 0x1A, 0x0A, 0, 0, 0, 0};

    private EventImageStorageService service(Path pasta) {
        EventImageStorageService s = new EventImageStorageService();
        ReflectionTestUtils.setField(s, "storagePath", pasta.toString());
        return s;
    }

    @Test
    @DisplayName("PNG de verdade e gravado e volta com a URL publica")
    void gravaImagem(@TempDir Path pasta) throws Exception {
        String url = service(pasta).saveImage(new MockMultipartFile("cover", "capa.png", "image/png", PNG), "event");

        assertThat(url).startsWith("/upload/events/event-").endsWith(".png");
        assertThat(Files.list(pasta)).hasSize(1);
    }

    @Test
    @DisplayName("HTML com nome de .png e recusado")
    void recusaHtmlDisfarcado(@TempDir Path pasta) {
        MockMultipartFile falso = new MockMultipartFile("cover", "capa.png", "image/png",
                "<html><script>alert(1)</script></html>".getBytes(StandardCharsets.UTF_8));

        assertThatThrownBy(() -> service(pasta).saveImage(falso, "event"))
                .isInstanceOf(InvalidEventDataException.class)
                .hasMessage("Este arquivo não é uma imagem de verdade.");
    }

    @Test
    @DisplayName("extensao fora da lista e recusada antes de ler")
    void recusaExtensao(@TempDir Path pasta) {
        assertThatThrownBy(() -> service(pasta).saveImage(new MockMultipartFile("cover", "capa.svg", "image/svg+xml", PNG), "event"))
                .isInstanceOf(InvalidEventDataException.class)
                .hasMessage("Envie a imagem em JPG, PNG ou WEBP.");
    }

    @Test
    @DisplayName("apagar pela URL so apaga o que foi gravado nesta pasta")
    void apagaSoDaPasta(@TempDir Path pasta) throws Exception {
        EventImageStorageService s = service(pasta);
        String url = s.saveImage(new MockMultipartFile("cover", "capa.png", "image/png", PNG), "event");

        s.deleteByUrl("/upload/images/outra-coisa.png");
        assertThat(Files.list(pasta)).hasSize(1);

        s.deleteByUrl(url);
        assertThat(Files.list(pasta)).isEmpty();
    }
}
