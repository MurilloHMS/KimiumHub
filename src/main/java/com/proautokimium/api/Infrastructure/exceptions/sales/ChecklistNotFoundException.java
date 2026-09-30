package com.proautokimium.api.Infrastructure.exceptions.sales;

import com.proautokimium.api.domain.exceptions.DomainException;
import org.springframework.http.HttpStatus;

/**
 * Também é a resposta para o checklist de outro vendedor: dizer "existe, mas
 * não é seu" confirmaria que o id é válido.
 */
public class ChecklistNotFoundException extends DomainException {
    public ChecklistNotFoundException() {
        super("Checklist não encontrado", HttpStatus.NOT_FOUND);
    }
}
