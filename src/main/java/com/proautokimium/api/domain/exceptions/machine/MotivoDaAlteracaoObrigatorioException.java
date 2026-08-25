package com.proautokimium.api.domain.exceptions.machine;

import com.proautokimium.api.domain.exceptions.DomainException;
import org.springframework.http.HttpStatus;

public class MotivoDaAlteracaoObrigatorioException extends DomainException {
    public MotivoDaAlteracaoObrigatorioException() {
        super("Informe o motivo da alteração da previsão de saída.", HttpStatus.BAD_REQUEST);
    }
}
