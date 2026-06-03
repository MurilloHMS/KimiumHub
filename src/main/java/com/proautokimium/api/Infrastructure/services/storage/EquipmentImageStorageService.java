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

/**
 * Serviço de armazenamento de imagens de {@link com.proautokimium.api.domain.entities.EquipmentGuide}.
 *
 * <p>
 * Segue exatamente a mesma lógica do {@link ProductImageStorageService},
 * mas com path de storage próprio para separar os arquivos.
 * </p>
 *
 * <p>
 * Configure em {@code application.properties}:
 * <pre>
 *   storage.equipment.image.path=/var/proauto/uploads/equipment
 * </pre>
 * </p>
 */
@Service
public class EquipmentImageStorageService {

    @Value("${storage.equipment.image.path}")
    private String storagePath;

    /**
     * Salva a imagem do equipamento em disco e retorna o path relativo para persistência.
     *
     * @param file          Arquivo enviado via multipart
     * @param equipmentName Nome do equipamento (usado como prefixo do arquivo)
     * @return Path relativo no formato {@code /upload/equipment/images/nome-uuid.ext}
     * @throws IOException caso a gravação falhe
     */
    public String saveImage(MultipartFile file, String equipmentName) throws IOException {
        String originalName = file.getOriginalFilename();
        String extension    = StringUtils.getFilenameExtension(originalName);

        if (extension == null || extension.isBlank()) {
            extension = "png";
        }

        // Sanitiza o nome para uso seguro em filesystem
        String safeName = equipmentName.replaceAll("[^a-zA-Z0-9_-]", "_");
        String filename  = safeName + "-" + UUID.randomUUID() + "." + extension;

        Path destination = Paths.get(storagePath).resolve(filename);
        Files.createDirectories(destination.getParent());
        Files.copy(file.getInputStream(), destination, StandardCopyOption.REPLACE_EXISTING);

        return "/upload/equipment/images/" + filename;
    }

    /**
     * Retorna o {@link Path} absoluto da imagem dado seu nome de arquivo.
     *
     * @param filename nome do arquivo (sem path)
     * @return {@link Path} absoluto
     */
    public Path searchImage(String filename) {
        return Paths.get(storagePath).resolve(filename);
    }
}