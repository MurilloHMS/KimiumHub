package com.proautokimium.api.domain.entities.sales;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * Um equipamento que pode ir em comodato. A Controladoria escolhe o produto do
 * Sankhya (decisão de 2026-09-29): código, nome e se está ativo vêm do ERP;
 * aqui ficam o nome do dia a dia, por onde o vendedor procura, e a ordem.
 */
@jakarta.persistence.Entity
@Table(name = "checklist_comodato_items")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ChecklistComodatoItem extends com.proautokimium.api.domain.abstractions.Entity {

    @Column(name = "product_code", nullable = false, unique = true)
    private int productCode;

    @Column(name = "popular_name", length = 100)
    private String popularName;

    @Column(name = "sort_order", nullable = false)
    private int sortOrder;

    @Column(name = "active", nullable = false)
    private boolean active = true;

    public ChecklistComodatoItem(int productCode, String popularName, int sortOrder) {
        this.productCode = productCode;
        update(popularName, sortOrder, true);
    }

    public void update(String popularName, int sortOrder, boolean active) {
        this.popularName = popularName == null || popularName.isBlank() ? null : popularName.strip();
        this.sortOrder = sortOrder;
        this.active = active;
    }
}
