package com.proautokimium.api.Infrastructure.services.storage;

import com.proautokimium.api.Infrastructure.abstractions.storage.FileStorage;
import com.proautokimium.api.Infrastructure.exceptions.events.EventExceptions.InvalidEventDataException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.util.Locale;
import java.util.Set;

/**
 * Capa dos eventos e foto dos palestrantes.
 *
 * <p>Servidas em {@code /upload/events/}, que é público: {@code <img>} não manda
 * JWT. Os nomes levam UUID, e o conteúdo é o que a própria tela interna mostra.
 */
@Service
public class EventImageStorageService extends FileStorage {

    public static final String RETURN_PATH = "/upload/events/";

    /** Uma foto de celular passa de 4 MB; capa acima de 8 é engano de arquivo. */
    public static final long MAX_BYTES = 8L * 1024 * 1024;

    private static final Set<String> EXTENSOES = Set.of("jpg", "jpeg", "png", "webp");

    @Value("${storage.events.image.path}")
    private String storagePath;

    @Override
    public String getStoragePath() {
        return storagePath;
    }

    @Override
    protected String getReturnPath() {
        return RETURN_PATH;
    }

    /**
     * Grava depois de conferir que é imagem de verdade.
     *
     * <p>Pela assinatura dos primeiros bytes, e não só pela extensão: a pasta é
     * servida publicamente, e um {@code .png} que é HTML vira página no domínio
     * da API.
     */
    public String saveImage(MultipartFile file, String prefix) throws IOException {
        if (file.getSize() > MAX_BYTES) {
            throw new InvalidEventDataException("A imagem passa de 8 MB.");
        }

        String extensao = StringUtils.getFilenameExtension(file.getOriginalFilename());
        if (extensao == null || !EXTENSOES.contains(extensao.toLowerCase(Locale.ROOT))) {
            throw new InvalidEventDataException("Envie a imagem em JPG, PNG ou WEBP.");
        }

        byte[] cabeca = new byte[12];
        int lidos;
        try (InputStream in = file.getInputStream()) {
            lidos = in.readNBytes(cabeca, 0, cabeca.length);
        }
        if (!isImage(cabeca, lidos)) {
            throw new InvalidEventDataException("Este arquivo não é uma imagem de verdade.");
        }

        return save(file, prefix);
    }

    /**
     * Apaga pela URL que a entidade guarda ({@code /upload/events/x.png}).
     *
     * <p>URL de outro lugar não é apagada: ela não foi gravada aqui.
     */
    public void deleteByUrl(String url) throws IOException {
        if (url == null || !url.startsWith(RETURN_PATH)) {
            return;
        }
        delete(url.substring(RETURN_PATH.length()));
    }

    static boolean isImage(byte[] b, int n) {
        boolean jpeg = n >= 3 && (b[0] & 0xFF) == 0xFF && (b[1] & 0xFF) == 0xD8 && (b[2] & 0xFF) == 0xFF;
        boolean png = n >= 8 && (b[0] & 0xFF) == 0x89 && b[1] == 'P' && b[2] == 'N' && b[3] == 'G';
        boolean webp = n >= 12 && b[0] == 'R' && b[1] == 'I' && b[2] == 'F' && b[3] == 'F'
                && b[8] == 'W' && b[9] == 'E' && b[10] == 'B' && b[11] == 'P';
        return jpeg || png || webp;
    }
}
