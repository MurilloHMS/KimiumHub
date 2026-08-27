package com.proautokimium.api.domain.enums;

/**
 * Como um modelo é carimbado numa pessoa.
 *
 * A diferença não é técnica, é de consequência — e é por isso que a tela
 * escreve as duas em português antes do clique.
 */
public enum ApplyMode {

    /**
     * Liga o que o modelo permite e **não desliga nada**.
     *
     * É o que faz "Vendas + Estoque" funcionar sem existir um modelo combinado:
     * o segundo carimbo não apaga o primeiro. Foi o caso que derrubou o desenho
     * de um grupo por pessoa.
     */
    SOMAR,

    /**
     * Grava exatamente o modelo, ligando e desligando.
     *
     * É o que "reaplicar" usa — e é o único caminho pelo qual um ajuste
     * individual se perde. A tela avisa antes, sempre.
     */
    SUBSTITUIR
}
