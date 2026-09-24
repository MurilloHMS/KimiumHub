package com.proautokimium.api.Infrastructure.abstractions.excel;

/**
 * Um objeto lido da planilha, <b>junto com a linha de onde ele veio</b>.
 *
 * <p>Existe porque o {@code getDataByExcel} devolve só a lista: quem precisa
 * dizer "confira a linha 47" não consegue usar o índice da lista, já que linhas
 * em branco são puladas e a partir da primeira delas os dois números divergem.
 *
 * @param linha o número como o Excel mostra na lateral, de 1 em diante — é o
 *              que a pessoa usa para achar a linha no arquivo dela
 */
public record ReadRow<T>(int linha, T valor) { }
