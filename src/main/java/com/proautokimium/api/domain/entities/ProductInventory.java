package com.proautokimium.api.domain.entities;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.core.util.Json;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.HashSet;
import java.util.Set;

@Entity
@DiscriminatorValue("INVENTORY")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ProductInventory extends ProductEntity {
    @Column(name = "minimum_stock")
    private int minimumStock;

    @JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
    @OneToMany(mappedBy = "product", fetch = FetchType.LAZY)
    private Set<MovementInventory> movements = new HashSet<>();
}
