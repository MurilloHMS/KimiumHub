package com.proautokimium.api.Infrastructure.services.storage;

import com.proautokimium.api.domain.exceptions.humanResources.InvalidRequestDataException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.UUID;

/**
 * Os arquivos dos documentos do funcionário, em disco.
 *
 * O nome original é LIMPO antes de virar caminho: "../../etc/passwd.pdf" ou
 * "a/b.pdf" escreveriam fora da pasta do funcionário. E todo caminho resolvido
 * é conferido contra a raiz — defesa dupla, porque `resolve` também recebe o
 * que veio do banco.
 */
@Service
public class EmployeeDocumentStorageService {

    private static final int MAX_NAME_LENGTH = 120;

    @Value("${storage.documents.path}")
    private String storagePath;

    public String save(byte[] content, String codParceiro, String originalFilename) throws IOException {
        String folder = safeName(codParceiro, "sem-codigo");
        String filename = UUID.randomUUID() + "-" + safeName(originalFilename, "documento");

        Path dir = inside(root().resolve(folder));
        Files.createDirectories(dir);
        Files.write(inside(dir.resolve(filename)), content);
        return folder + "/" + filename;
    }

    public Path resolve(String relativePath){
        return inside(root().resolve(relativePath));
    }

    /** Desfaz um `save` recusado, ou apaga o arquivo de um documento excluído. */
    public void delete(String relativePath) throws IOException{
        Files.deleteIfExists(resolve(relativePath));
    }

    /**
     * Só o último pedaço do nome (o navegador às vezes manda o caminho inteiro),
     * e só letra, número, ponto, hífen, sublinhado e espaço — o resto vira "_".
     */
    static String safeName(String name, String fallback){
        if(name == null || name.isBlank()) return fallback;

        String lastPart = name.substring(Math.max(name.lastIndexOf('/'), name.lastIndexOf('\\')) +1);
        String clean = lastPart.replaceAll("[^\\p{L}\\p{N}._-]","_").replaceAll("^\\.+", "").trim();
        if(clean.isEmpty()) return fallback;

        return clean.length() > MAX_NAME_LENGTH ? clean.substring(clean.length() - MAX_NAME_LENGTH) : clean;
    }

    private Path root(){
        return Paths.get(storagePath).toAbsolutePath().normalize();
    }

    private Path inside(Path path){
        Path normalized = path.toAbsolutePath().normalize();
        if(!normalized.startsWith(root())){
            throw new InvalidRequestDataException("Caminho de arquivo invalido");
        }
        return normalized;
    }
}
