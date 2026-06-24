package com.proautokimium.api.Infrastructure.services.storage;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.UUID;

@Service
public class HoleriteStorageService {

    @Value("${storage.holerite.path}")
    private String storagePath;

    /** Salva o PDF do holerite em /funcionario/holerite/{codParceiro}/ e retorna o caminho relativo. */
    public String save(byte[] content, String codParceiro, LocalDate competencia) throws IOException {
        String comp = competencia.format(DateTimeFormatter.ofPattern("yyyy-MM"));
        String filename = comp + "-" + UUID.randomUUID() + ".pdf";

        Path dir = Paths.get(storagePath, codParceiro);
        Files.createDirectories(dir);
        Files.write(dir.resolve(filename), content);

        return codParceiro + "/" + filename;
    }

    /** Resolve o caminho absoluto do arquivo a partir do caminho relativo salvo. */
    public Path resolve(String relativePath) {
        return Paths.get(storagePath).resolve(relativePath);
    }
}
