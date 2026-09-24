package com.proautokimium.api.Application.DTOs.fuelsupply;

import java.util.List;

/**
 * O que a gravação fez de verdade.
 *
 * <p>Existe porque a resposta anterior era a frase fixa "Importação concluída
 * com sucesso!" — devolvida <b>mesmo quando nada era gravado</b>, já que o
 * {@code insertByRange} capturava a exceção por dentro e o controller
 * descartava o retorno dele.
 *
 * <p><b>É tudo ou nada.</b> Uma linha recusada cancela a remessa inteira e
 * {@code gravadas} vem zero. Gravar parte e recusar o resto obrigaria a pessoa
 * a descobrir sozinha quais das 250 entraram antes de reenviar — e reenviar a
 * planilha toda duplicaria as que já estavam lá.
 *
 * @param motivos uma frase por linha recusada, com o número da linha. Só os
 *                problemas: listar as 246 que passaram afogaria as 2 que não
 */
public record FuelSupplyImportResultDTO(
        int gravadas,
        int recusadas,
        List<String> motivos
) { }
