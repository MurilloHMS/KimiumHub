package com.proautokimium.api.domain.entities;

import com.proautokimium.api.domain.entities.humanResources.Company;
import com.proautokimium.api.domain.entities.humanResources.Team;
import com.proautokimium.api.domain.enums.Department;
import com.proautokimium.api.domain.enums.Hierarchy;
import jakarta.persistence.Column;
import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;

@Entity
@DiscriminatorValue("FUNCIONARIO")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Employee extends Partner {
    @Column(name = "codigo_gerente", length = 9)
    private String codigoGerente;

    @Enumerated(EnumType.STRING)
    @Column(name = "hierarquia", length = 15)
    private Hierarchy hierarquia;

    @Column(name = "data_aniversario")
    private LocalDate birthday;

    @Enumerated(EnumType.STRING)
    @Column(name = "departamento")
    private Department department;

    // company/team: cadastros novos (Estrutura Organizacional). hierarquia/department
    // continuam como enum por enquanto — migração pra FK fica pra uma branch própria,
    // já que também são usados pelo módulo de FuelSupply.
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "company_id")
    private Company company;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "team_id")
    private Team team;
}
