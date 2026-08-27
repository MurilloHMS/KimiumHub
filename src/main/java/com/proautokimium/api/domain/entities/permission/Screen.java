package com.proautokimium.api.domain.entities.permission;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Uma tela do ERP, do jeito que o controle de acesso a conhece.
 *
 * O id **é a rota** — `stock/movements`, não um número. A authority fica
 * `stock/movements:EXCLUIR`, e é isso que aparece no log, no `@PreAuthorize` e
 * na mensagem do 403; com id numérico, todo diagnóstico exigiria uma consulta
 * para descobrir que a tela 37 é a de movimentações.
 */
@Entity
@Table(name = "screens")
@Getter @Setter
@NoArgsConstructor
public class Screen {

    @Id
    @Column(name = "code", length = 120)
    private String code;

    @Column(name = "label", length = 120, nullable = false)
    private String label;

    /** Agrupamento visual do grid, e nada mais. */
    @Column(name = "module", length = 60, nullable = false)
    private String module;

    /** Vai de 10 em 10 no seed para caber tela nova no meio. */
    @Column(name = "sort_order", nullable = false)
    private int sortOrder;

    @Column(name = "active", nullable = false)
    private boolean active = true;
}
