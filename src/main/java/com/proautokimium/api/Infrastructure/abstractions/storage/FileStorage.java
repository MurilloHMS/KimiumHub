package com.proautokimium.api.Infrastructure.abstractions.storage;


import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.UUID;

public abstract class FileStorage {

    protected abstract String getStoragePath();
    protected abstract String getReturnPath();

    protected String buildFileName(MultipartFile file, String prefix){
        String extension = StringUtils.getFilenameExtension(file.getOriginalFilename());

        if(extension == null || extension.isBlank()){
            extension = "bin";
        }

        return prefix + "-" + UUID.randomUUID() + "-" + extension;
    }

    public String save(MultipartFile file, String prefix) throws IOException{
        String filename = buildFileName(file, prefix);

        Path destination = Paths.get(getStoragePath()).resolve(filename);
        Files.createDirectories(destination.getParent());
        Files.copy(file.getInputStream(), destination, StandardCopyOption.REPLACE_EXISTING);

        return getReturnPath() + filename;
    }

    public Path searchFile(String filename){
        return Paths.get(getStoragePath()).resolve(filename);
    }
}
