package com.proautokimium.api.domain.entities;

import com.proautokimium.api.domain.entities.humanResources.Company;
import com.proautokimium.api.domain.entities.humanResources.Hierarchy;
import com.proautokimium.api.domain.entities.humanResources.Team;
import com.proautokimium.api.domain.enums.Department;
import com.proautokimium.api.domain.enums.humanResources.TransportType;
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

import java.math.BigDecimal;
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

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "hierarchy_id")
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

    @Column(name = "vacation_balance_days")
    private Integer vacationBalanceDays;

    @Column(name = "daily_commutes_count")
    private Integer dailyCommutesCount;

    @Column(name = "daily_meals_count")
    private Integer dailyMealsCount;

    @Enumerated(EnumType.STRING)
    @Column(name = "transport_type", length = 25)
    private TransportType transportType;

    @Column(name = "ticket_price", precision = 10, scale = 2)
    private BigDecimal ticketPrice;

    @Column(name = "vehicle_km_per_liter", precision = 6, scale = 2)
    private BigDecimal vehicleKmPerLiter;

    @Column(name = "daily_distance_km", precision = 8, scale = 2)
    private BigDecimal dailyDistanceKm;
}
