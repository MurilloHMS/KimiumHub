package com.proautokimium.api.controllers.processoSeletivo;

import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StringUtils;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Locale;

/**
 * Monta a resposta de download de currículo, e só ela.
 *
 * <p>Existe porque <b>três</b> endpoints servem o mesmo arquivo — o download
 * interno antigo, o interno novo por id, e o do próprio candidato pelo token —
 * e a regra de segurança não pode divergir entre eles. Copiada em três lugares,
 * o dia em que alguém acrescenta o quarto é o dia em que um deles fica sem
 * {@code nosniff}.
 */
public final class CurriculoResponse {

    private CurriculoResponse() { }

    public static ResponseEntity<Resource> de(Path caminho, String nomeExibido) throws IOException {
        if (caminho == null || !Files.exists(caminho)) {
            // Registro órfão — linha apontando para arquivo que sumiu do disco —
            // já é caso real neste projeto, e 404 é a resposta honesta: 503
            // diria que o servidor falhou.
            return ResponseEntity.notFound().build();
        }

        Resource recurso = new UrlResource(caminho.toUri());
        String tipo = tipoPor(nomeExibido);

        // Prévia no navegador só para PDF de verdade. Qualquer outra coisa desce
        // como anexo: arquivo de tipo desconhecido servido inline, a partir da
        // origem da API, roda no contexto da sessão de quem clicou.
        boolean ehPdf = MediaType.APPLICATION_PDF_VALUE.equals(tipo);
        String disposicao = (ehPdf ? "inline" : "attachment") + "; filename=\"" + nomeExibido + "\"";

        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(tipo))
                .header(HttpHeaders.CONTENT_DISPOSITION, disposicao)
                // Sem isto o navegador adivinha o tipo pelo conteúdo e ignora o
                // cabeçalho acima, que é justamente o que a linha anterior tenta
                // impedir.
                .header("X-Content-Type-Options", "nosniff")
                .body(recurso);
    }

    /**
     * O tipo sai daqui, e não de {@code Files.probeContentType} — que consulta
     * o sistema operacional, volta {@code null} com frequência em container, e
     * cujo fallback anterior rotulava <b>qualquer</b> arquivo como PDF.
     */
    private static String tipoPor(String nomeArquivo) {
        String extensao = StringUtils.getFilenameExtension(nomeArquivo);
        extensao = extensao == null ? "" : extensao.toLowerCase(Locale.ROOT);

        return switch (extensao) {
            case "pdf"  -> MediaType.APPLICATION_PDF_VALUE;
            case "doc"  -> "application/msword";
            case "docx" -> "application/vnd.openxmlformats-officedocument.wordprocessingml.document";
            default     -> MediaType.APPLICATION_OCTET_STREAM_VALUE;
        };
    }
}
