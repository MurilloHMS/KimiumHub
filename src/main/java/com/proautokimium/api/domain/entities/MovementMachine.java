package com.proautokimium.api.domain.entities;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "machine_movements")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class MovementMachine extends com.proautokimium.api.domain.abstractions.Entity {

    @Column(name = "movement_date", nullable = false)
    private LocalDateTime movementDate;

    @Column(name = "quantity", nullable = false)
    private int quantity;

    @ManyToOne
    @JoinColumn(name = "machine_id")
    private ProductMachine machine;
}
