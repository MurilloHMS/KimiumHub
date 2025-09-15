package com.proautokimium.api.domain.entities;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDate;

@Entity
@Table(name = "products_movements")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class MovementInventory extends com.proautokimium.api.domain.abstractions.Entity implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;

    @Column(name = "movement_date", nullable = false)
    private LocalDate movementDate;

    @ManyToOne
    @JoinColumn(name = "product_id")
    private ProductInventory product;
}
