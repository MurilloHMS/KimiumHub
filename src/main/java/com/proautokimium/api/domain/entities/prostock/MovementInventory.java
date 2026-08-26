package com.proautokimium.api.domain.entities.prostock;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import org.hibernate.annotations.ColumnDefault;
import org.hibernate.annotations.Generated;
import org.hibernate.generator.EventType;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;

@Entity
@Table(name = "products_movements")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class MovementInventory extends com.proautokimium.api.domain.abstractions.Entity implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;

    /** Quando a movimentação aconteceu. Informado por quem lançou, e editável. */
    @Column(name = "movement_date", nullable = false)
    private LocalDateTime movementDate;

    /**
     * Quando foi registrada — e é por esta que o estoque atual é lido.
     *
     * Quem preenche é o **banco**, não o Java: o desktop continua no ar e
     * escreve nesta mesma tabela. Um `@CreatedDate` do Spring valeria só para
     * quem passa por aqui, e a ordem voltaria a mentir quando os dois clientes
     * lançassem no mesmo dia.
     *
     * `insertable = false` para o Hibernate não sobrescrever o DEFAULT, e
     * `@Generated` para ele ler de volta o valor que o banco gravou.
     */
    @Column(name = "created_at", nullable = false, insertable = false, updatable = false)
    // `now()` aqui e `clock_timestamp()` na V83, de propósito: esta anotação só
    // vale para o schema que o Hibernate gera nos testes, onde o banco é H2 e
    // `clock_timestamp()` não existe. Quem manda em produção é a migration.
    @ColumnDefault("now()")
    @Generated(event = EventType.INSERT)
    private OffsetDateTime createdAt;

    @Column(name = "quantity", nullable = false)
    private int quantity;

    @ManyToOne
    @JoinColumn(name = "product_id")
    private ProductInventory product;
}
