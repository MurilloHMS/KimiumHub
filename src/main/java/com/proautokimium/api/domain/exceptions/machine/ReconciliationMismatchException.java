package com.proautokimium.api.domain.exceptions.machine;

import com.proautokimium.api.domain.exceptions.DomainException;
import org.springframework.http.HttpStatus;

/**
 * O movimento e as programações escolhidas discordam.
 *
 * É o erro mais importante deste módulo. Aceitar uma conciliação que não fecha
 * criaria exatamente a divergência que ela existe para evitar — e nasceria
 * escondida, porque os dois lançamentos pareceriam corretos olhando separado.
 *
 * A mensagem é montada por quem lança, com o número que não bateu: "saída de 3
 * precisa de 3 programações, e vieram 2" resolve sozinho; "dados inválidos"
 * manda a pessoa adivinhar.
 */
public class ReconciliationMismatchException extends DomainException {
    public ReconciliationMismatchException(String message) {
        super(message, HttpStatus.BAD_REQUEST);
    }
}
