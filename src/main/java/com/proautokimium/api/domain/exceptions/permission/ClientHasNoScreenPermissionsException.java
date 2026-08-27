package com.proautokimium.api.domain.exceptions.permission;

import com.proautokimium.api.domain.exceptions.DomainException;
import org.springframework.http.HttpStatus;

/**
 * Cliente não participa do controle por tela.
 *
 * O portal dele tem sessão e escopo próprios, decididos pela API, e ele não tem
 * linha em `user_permissions` — a V86 o excluiu de propósito. Recusar aqui é o
 * que impede a tela de configuração de criar essas linhas pela porta dos
 * fundos e passar a sugerir que o portal responde a elas.
 */
public class ClientHasNoScreenPermissionsException extends DomainException {
    public ClientHasNoScreenPermissionsException() {
        super("Clientes não têm permissões de tela — o portal do cliente tem escopo próprio.",
                HttpStatus.UNPROCESSABLE_ENTITY);
    }
}
