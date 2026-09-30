package com.proautokimium.api.domain.entities.sales;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * Um item de comunicação visual (guia, adesivo, pop-up). A Controladoria mantém
 * a lista; item que sai é desativado, não apagado, porque checklists antigos o
 * citam.
 */
@jakarta.persistence.Entity
@Table(name = "checklist_visual_items")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ChecklistVisualItem extends com.proautokimium.api.domain.abstractions.Entity {

    @Column(name = "name", nullable = false, unique = true, length = 150)
    private String name;

    @Column(name = "sort_order", nullable = false)
    private int sortOrder;

    @Column(name = "active", nullable = false)
    private boolean active = true;

    public ChecklistVisualItem(String name, int sortOrder) {
        update(name, sortOrder, true);
    }

    public void update(String name, int sortOrder, boolean active) {
        this.name = name.strip();
        this.sortOrder = sortOrder;
        this.active = active;
    }
}
