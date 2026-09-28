package com.proautokimium.api.Infrastructure.exceptions.humanResources;

import com.proautokimium.api.domain.exceptions.DomainException;
import org.springframework.http.HttpStatus;

/**
 * O PDF passou do que um servidor de e-mail aceita. Acontece com o comprovante
 * de um funcionário e muitas fotos anexadas. A saída é baixar o arquivo.
 */
public class ReportTooLargeForEmailException extends DomainException {
    public ReportTooLargeForEmailException(long megabytes, long limitMegabytes) {
        super("O PDF tem " + megabytes + " MB, acima do limite de " + limitMegabytes
                + " MB para e-mail. Baixe o arquivo ou diminua o período.", HttpStatus.UNPROCESSABLE_ENTITY);
    }
}
