package com.proautokimium.api.Infrastructure.services.storage;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.UUID;

@Service
public class MedicalCertificateStorageService {

    @Value("${storage.medical-certificates.path}")
    private String storagePath;

    public String save(byte[] content, String codParceiro, String originalFilename) throws IOException {
        String filename = UUID.randomUUID() + "-" + originalFilename;
        Path dir = Paths.get(storagePath, codParceiro);
        Files.createDirectories(dir);
        Files.write(dir.resolve(filename), content);
        return codParceiro + "/" + filename;
    }

    public Path resolve(String relativePath) {
        return Paths.get(storagePath).resolve(relativePath);
    }
}
