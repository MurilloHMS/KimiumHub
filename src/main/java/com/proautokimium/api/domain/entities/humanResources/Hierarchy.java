package com.proautokimium.api.domain.entities.humanResources;

import com.proautokimium.api.domain.abstractions.Entity;
import jakarta.persistence.Column;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@jakarta.persistence.Entity
@Table(name = "hierarchies")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Hierarchy extends Entity {

    @Column(name = "name", length = 50, nullable = false, unique = true)
    private String name;

    @Column(name = "level_order", nullable = false)
    private Integer levelOrder;
}
