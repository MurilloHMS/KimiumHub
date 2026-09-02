package com.proautokimium.api.Infrastructure.services.storage;

import com.proautokimium.api.Infrastructure.abstractions.storage.FileStorage;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class SignatureBackgroundStorageService extends FileStorage {

    @Value("${storage.signature.path}")
    private String storagePath;

    @Override
    protected String getStoragePath() {
        return storagePath;
    }

    /**
     * O prefixo da URL, e ele tem que casar com o `addResourceHandler` do
     * `StaticResourceConfig` — `FileStorage.save` devolve `getReturnPath() +
     * filename`, então a barra final não é enfeite: sem ela o nome do arquivo
     * cola no caminho e vira `/upload/signaturefoto.png`.
     */
    @Override
    protected String getReturnPath() {
        return "/upload/signature/";
    }
}
