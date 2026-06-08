package com.proautokimium.api.Infrastructure.services.storage;

import com.proautokimium.api.Infrastructure.abstractions.storage.FileStorage;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

@Service
public class StorageService extends FileStorage {
    @Value("${storage.curriculos.path}")
    private String storagePath;

    @Override
    protected String getStoragePath() {
        return storagePath;
    }

    @Override
    protected String getReturnPath() {
        return "";
    }

    @Override
    protected String buildFileName(MultipartFile file, String prefix) {
        String extension = StringUtils.getFilenameExtension(file.getOriginalFilename());

        return prefix + "." + extension;
    }
}
