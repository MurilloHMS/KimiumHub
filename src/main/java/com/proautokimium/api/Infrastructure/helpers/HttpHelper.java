package com.proautokimium.api.Infrastructure.helpers;

import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;

public class HttpHelper {

    /**
     * Cria o header de retorno dos arquivos PDF
     * @param filename Nome do arquivo
     * @param length Tamanho do arquivo
     * @return Retorna header com contentType application/pdf
     */
    public static HttpHeaders createPdfHeader(String filename, int length){
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_PDF);
        headers.setContentLength(length);
        headers.setContentDisposition(
                ContentDisposition.attachment()
                        .filename(filename)
                        .build()
        );
        return headers;
    }
}
