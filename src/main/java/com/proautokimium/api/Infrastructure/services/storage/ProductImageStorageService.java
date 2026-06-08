package com.proautokimium.api.Infrastructure.services.storage;

import com.proautokimium.api.Infrastructure.abstractions.storage.FileStorage;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class ProductImageStorageService extends FileStorage {

    @Value("${storage.image.path}")
    private String storagePath;


    @Override
    protected String getStoragePath() {
        return storagePath;
    }

    @Override
    protected String getReturnPath() {
        return "/upload/images/";
    }
}