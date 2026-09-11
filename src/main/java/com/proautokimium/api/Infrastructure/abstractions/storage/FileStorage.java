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
        return buildFileName(file.getOriginalFilename(), prefix);
    }

    protected String buildFileName(String originalFileName, String prefix){
        String extension = StringUtils.getFilenameExtension(originalFileName);

        if(extension == null || extension.isBlank()){
            extension = "bin";
        }

        return prefix + "-" + UUID.randomUUID() + "." + extension;
    }

    public String save(MultipartFile file, String prefix) throws IOException{
        String filename = buildFileName(file, prefix);

        Path destination = resolverDentroDaPasta(filename);
        Files.createDirectories(destination.getParent());
        Files.copy(file.getInputStream(), destination, StandardCopyOption.REPLACE_EXISTING);

        return getReturnPath() + filename;
    }

    /**
     * Grava bytes que já estão em memória.
     *
     * Existe porque a foto de produto passou a poder vir da galeria, e lá o
     * arquivo é lido do disco como `byte[]` — não existe `MultipartFile` no
     * caminho. As duas entradas produzem o mesmo nome e o mesmo destino, então
     * nada a jusante precisa saber de onde a imagem veio.
     */
    public String save(byte[] content, String originalFilename, String prefix) throws IOException{
        String filename = buildFileName(originalFilename, prefix);

        Path destination = resolverDentroDaPasta(filename);
        Files.createDirectories(destination.getParent());
        Files.write(destination, content);

        return getReturnPath() + filename;
    }

    public Path searchFile(String filename){
        return resolverDentroDaPasta(filename);
    }

    /**
     * Resolve o nome dentro da pasta de armazenamento, e recusa o que sair dela.
     *
     * <p>Até 2026-09-11 isto era um {@code resolve} cru, e o {@code filename}
     * chega de {@code @PathVariable} no download de currículo. Está atrás de
     * authority, então não era anônimo — mas qualquer conta do RH lia qualquer
     * arquivo que a JVM alcançasse.
     *
     * <p><b>Duas formas de sair, e a segunda passa despercebida:</b>
     * {@code ../..}, e o <b>caminho absoluto</b> — {@code Path.resolve} com um
     * absoluto descarta a base inteira e devolve o absoluto, sem nenhum ponto
     * envolvido. O {@code normalize} nos dois lados cobre as duas.
     *
     * <p>A guarda vive aqui, na base, e não numa subclasse: são onze
     * implementações, e consertar uma deixaria dez abertas.
     */
    private Path resolverDentroDaPasta(String filename){
        Path base = Paths.get(getStoragePath()).toAbsolutePath().normalize();
        Path alvo = base.resolve(filename).normalize();

        if(!alvo.startsWith(base)){
            throw new IllegalArgumentException("Nome de arquivo inválido: " + filename);
        }

        return alvo;
    }
}
