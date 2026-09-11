package com.proautokimium.api.controllers.processoSeletivo;

import com.proautokimium.api.Infrastructure.services.storage.StorageService;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Locale;

@RestController
@RequestMapping("api/curriculos")
public class CurriculoController {

    private final StorageService storageService;

    public CurriculoController(StorageService storageService){
        this.storageService = storageService;
    }

    @PreAuthorize("hasAuthority('rh/candidaturas:BAIXAR')")
    @GetMapping("/{fileName}")
    public ResponseEntity<Resource> getCurriculo(@PathVariable String fileName) throws IOException {
        Path path = storageService.searchFile(fileName);

        if(!Files.exists(path))
            return ResponseEntity.notFound().build();

        Resource resource = new UrlResource(path.toUri());
        String contentType = tipoPor(fileName);

        // Previa no navegador so para PDF de verdade. Qualquer outra coisa desce
        // como anexo: arquivo de tipo desconhecido servido inline, a partir da
        // origem da API, roda no contexto da sessao de quem clicou.
        boolean ehPdf = MediaType.APPLICATION_PDF_VALUE.equals(contentType);
        String disposicao = (ehPdf ? "inline" : "attachment") + "; filename=\"" + fileName + "\"";

        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(contentType))
                .header(HttpHeaders.CONTENT_DISPOSITION, disposicao)
                // Sem isto o navegador adivinha o tipo pelo conteudo e ignora o
                // cabecalho acima, que e justamente o que a linha anterior tenta
                // impedir. E a linha de maior retorno por caractere deste arquivo.
                .header("X-Content-Type-Options", "nosniff")
                .body(resource);
    }

    /**
     * O tipo sai de um mapa nosso, e nao de {@code Files.probeContentType}.
     *
     * <p>O {@code probeContentType} consulta o sistema operacional, volta
     * {@code null} com frequencia em container, e o fallback anterior rotulava
     * <b>qualquer</b> arquivo como {@code application/pdf} — inclusive o
     * {@code .docx} que existe no disco de antes de haver validacao.
     */
    private static String tipoPor(String fileName) {
        String extensao = StringUtils.getFilenameExtension(fileName);
        extensao = extensao == null ? "" : extensao.toLowerCase(Locale.ROOT);

        return switch (extensao) {
            case "pdf"  -> MediaType.APPLICATION_PDF_VALUE;
            case "doc"  -> "application/msword";
            case "docx" -> "application/vnd.openxmlformats-officedocument.wordprocessingml.document";
            default     -> MediaType.APPLICATION_OCTET_STREAM_VALUE;
        };
    }
}
