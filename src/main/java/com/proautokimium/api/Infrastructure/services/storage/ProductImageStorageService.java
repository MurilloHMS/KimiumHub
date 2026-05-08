package com.proautokimium.api.Infrastructure.services.storage;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.UUID;

@Service
public class ProductImageStorageService {

    @Value("${storage.image.path}")
    private String storagePath;

    public String saveImage(MultipartFile file, String productCode) throws IOException {
        String originalName = file.getOriginalFilename();
        String extension = StringUtils.getFilenameExtension(originalName);

        if (extension == null || extension.isBlank()) {
            extension = "png";
        }

        String filename = productCode + "-" + UUID.randomUUID() + "." + extension;

        Path destination = Paths.get(storagePath).resolve(filename);
        Files.createDirectories(destination.getParent());
        Files.copy(file.getInputStream(), destination, StandardCopyOption.REPLACE_EXISTING);

        return "/upload/images/" + filename;
    }

    public Path searchImage(String filename) {
        return Paths.get(storagePath).resolve(filename);
    }
}