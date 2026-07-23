package com.proautokimium.api.domain.entities.humanResources;

import jakarta.persistence.Column;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@jakarta.persistence.Entity
@Table(name = "positions")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Position extends com.proautokimium.api.domain.abstractions.Entity {

    @Column(name = "name", length = 100, nullable = false, unique = true)
    private String name;
}
