package com.proautokimium.api.Infrastructure.helpers;

import com.proautokimium.api.Infrastructure.utils.FileNameSanitizer;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.Set;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

public class ZipHelper {

    private ZipHelper() {}

    /**
     * Monta um ZIP a partir de arquivos já em memória.
     *
     * UTF-8 explícito: sem isso o Windows abre "JOÃO" como "JOÃO". É o padrão
     * do Java, mas deixar escrito evita que alguém "simplifique" a chamada.
     */
    public static byte[] zip(Map<String, byte[]> files) throws IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();

        try(ZipOutputStream zip = new ZipOutputStream(out, StandardCharsets.UTF_8)) {
            for(Map.Entry<String, byte[]> file : files.entrySet()) {
                zip.putNextEntry(new ZipEntry(file.getKey()));
                zip.write(file.getValue());
                zip.closeEntry();
            }
        }
        return out.toByteArray();
    }

    /**
     * Devolve um nome de arquivo que ainda não está no ZIP.
     *
     * Dois homônimos na mesma turma produziriam a mesma entrada, e
     * `ZipOutputStream` **lança exceção** em entrada duplicada — não sobrescreve
     * em silêncio. O lote inteiro morreria por causa de dois "JOÃO SILVA".
     */
    public static String uniqueName(String base, Set<String> used){
        String safe = FileNameSanitizer.sanitize(base);
        String name = safe + ".pdf";

        int counter = 2;
        while(used.contains(name)){
            name = safe + "(" + counter + ").pdf";
            counter++;
        }
        return name;
    }
}
