package com.proautokimium.api.Infrastructure.services.storage;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.Normalizer;
import java.util.UUID;

@Service
public class RentalReceiptStorageService {

    @Value("${storage.rental-receipts.path}")
    private String storagePath;

    public String save(byte[] pdfContent, int year, String month, String codMatriz, String name) throws IOException {
        String sanitized = sanitize(name);
        String filename = codMatriz + "_" + sanitized + "_" + UUID.randomUUID().toString().substring(0, 8) + ".pdf";

        Path dir = Paths.get(storagePath, String.valueOf(year), month);
        Files.createDirectories(dir);
        Files.write(dir.resolve(filename), pdfContent);

        return year + "/" + month + "/" + filename;
    }

    public Path resolve(String relativePath) {
        return Paths.get(storagePath).resolve(relativePath);
    }

    private String sanitize(String input) {
        String normalized = Normalizer.normalize(input, Normalizer.Form.NFD)
                .replaceAll("[\\p{InCombiningDiacriticalMarks}]", "");
        return normalized.replaceAll("[^a-zA-Z0-9]", "_")
                .replaceAll("_+", "_")
                .replaceAll("^_|_$", "");
    }
}
