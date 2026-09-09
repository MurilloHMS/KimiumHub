package com.proautokimium.api.domain.exceptions.partners;

import com.proautokimium.api.domain.exceptions.DomainException;
import org.springframework.http.HttpStatus;

/**
 * Já existe funcionário com aquele código de parceiro.
 *
 * <p><b>409, e não 400.</b> A requisição está certa; o que impede é o estado do
 * banco. É a mesma distinção da {@code CustomerAlreadyExistsException}, e é o
 * que deixa a tela dizer "esse código já é de alguém" em vez de "dados
 * inválidos".
 *
 * <p>O código de parceiro é o CODPARC do Sankhya, onde ele é chave da
 * {@code TGFPAR}. Dois funcionários com o mesmo código não são um cadastro
 * duplicado qualquer: são duas linhas apontando para o mesmo parceiro do ERP, e
 * a partir daí toda busca por código devolve a errada — sem erro nenhum.
 */
public class EmployeeAlreadyExistsException extends DomainException {

    public EmployeeAlreadyExistsException(String codParceiro) {
        super("Já existe um funcionário com o código de parceiro " + codParceiro, HttpStatus.CONFLICT);
    }
}
