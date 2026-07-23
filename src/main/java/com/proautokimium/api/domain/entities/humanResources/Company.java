package com.proautokimium.api.domain.entities.humanResources;

import com.proautokimium.api.domain.abstractions.Entity;
import jakarta.persistence.Column;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@jakarta.persistence.Entity
@Table(name = "companies")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Company extends Entity {

    @Column(name = "name", length = 100, nullable = false)
    private String name;

    @Column(name = "legal_name", length = 150, nullable = false)
    private String legalName;

    @Column(name = "cnpj", length = 18, nullable = false, unique = true)
    private String cnpj;
}
