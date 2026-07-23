package com.proautokimium.api.domain.entities.humanResources;

import com.proautokimium.api.domain.abstractions.Entity;
import jakarta.persistence.Column;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@jakarta.persistence.Entity
@Table(name = "departments")
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class Department extends Entity {

    @Column(name = "name", length = 100, nullable = false, unique = true)
    private String name;
}
