package com.proautokimium.api.Infrastructure.services.storage;

import com.proautokimium.api.Infrastructure.abstractions.storage.FileStorage;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

@Service
public class EquipmentImageStorageService extends FileStorage {

    @Value("${storage.equipment.image.path}")
    private String storagePath;

    @Override
    public String getStoragePath() {
        return storagePath;
    }

    @Override
    protected String getReturnPath() {
        return "/upload/equipment/images/";
    }

    @Override
    protected String buildFileName(MultipartFile file, String equipmentName) {
        String safeName = equipmentName.replaceAll("[^a-zA-Z0-9_-]", "_");

        return super.buildFileName(file, safeName);
    }
}