package com.proautokimium.api.Infrastructure.services.storage;

import com.proautokimium.api.Infrastructure.abstractions.storage.FileStorage;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class GalleryStorageService extends FileStorage {
    @Value("${storage.gallery.path}")
    private String path;

    @Override
    protected String getStoragePath() {
        return path;
    }

    @Override
    protected String getReturnPath() {
        return "/upload/gallery/";
    }
}
