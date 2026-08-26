package com.proautokimium.api.Application.DTOs.prostock.machine;

import java.util.UUID;

/**
 * As duas contagens da mesma máquina, lado a lado.
 *
 * `stock` vem de `products_movements`; `scheduled` é quantas linhas de
 * programação estão em estoque. São **dois caminhos para o mesmo fato**, e
 * manter os dois foi decisão de projeto — com o custo assumido de que todo
 * caminho novo precisa lembrar de conciliar.
 *
 * Este DTO existe para o dia em que alguém esquecer. Sem ele, a divergência só
 * aparece contando na mão.
 */
public record MachineDivergenceDTO(
        UUID machineId,
        String systemCode,
        String name,
        int stock,
        int scheduled
) {
    /** Positivo, sobra no estoque; negativo, sobra na programação. */
    public int difference() {
        return stock - scheduled;
    }

    public boolean diverges() {
        return stock != scheduled;
    }
}
