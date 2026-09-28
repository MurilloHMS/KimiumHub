package com.proautokimium.api.Infrastructure.exceptions.humanResources;

import com.proautokimium.api.domain.exceptions.DomainException;
import org.springframework.http.HttpStatus;

/**
 * O funcionário já tem férias pedidas ou aprovadas que cruzam o período.
 *
 * Separada de {@link OverlappingVacationRequestException}, que fala do SETOR:
 * a mensagem precisa dizer de quem é o conflito, senão a pessoa procura um
 * colega de férias que não existe.
 */
public class OwnVacationOverlapException extends DomainException {
    public OwnVacationOverlapException() {
        super("Já existem férias pedidas ou aprovadas para este funcionário nesse período", HttpStatus.CONFLICT);
    }
}
