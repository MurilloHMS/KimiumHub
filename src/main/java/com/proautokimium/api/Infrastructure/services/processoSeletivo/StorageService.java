package com.proautokimium.api.Infrastructure.services.processoSeletivo;

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
public class StorageService {
    @Value("${storage.curriculos.path}")
    private String storagePath;

    public String salvarCurriculo(MultipartFile file, UUID candidatoId) throws IOException {
        String extensao = StringUtils.getFilenameExtension(file.getOriginalFilename());
        String nomeArquivo = candidatoId + "." + extensao;

        Path destino = Paths.get(storagePath).resolve(nomeArquivo);
        Files.createDirectories(destino.getParent());
        Files.copy(file.getInputStream(), destino, StandardCopyOption.REPLACE_EXISTING);

        return nomeArquivo;
    }

    public Path buscarCurriculo(String nomeArquivo){
        return Paths.get(storagePath).resolve(nomeArquivo);
    }
}
