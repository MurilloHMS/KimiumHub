package com.proautokimium.api.domain.entities.humanResources;

import com.proautokimium.api.domain.entities.Employee;
import com.proautokimium.api.domain.enums.humanResources.CareerChangeReason;
import com.proautokimium.api.domain.enums.humanResources.ContractType;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;

@jakarta.persistence.Entity
@Table(name = "career_histories")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class CareerHistory extends com.proautokimium.api.domain.abstractions.Entity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "employee_id", nullable = false)
    private Employee employee;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "position_id", nullable = false)
    private Position position;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "position_level_id", nullable = false)
    private PositionLevel positionLevel;

    @Column(name = "salary", nullable = false, precision = 10, scale = 2)
    private BigDecimal salary;

    @Enumerated(EnumType.STRING)
    @Column(name = "contract_type", nullable = false, length = 10)
    private ContractType contractType;

    @Enumerated(EnumType.STRING)
    @Column(name = "reason", nullable = false, length = 40)
    private CareerChangeReason reason;

    @Column(name = "effective_date", nullable = false)
    private LocalDate effectiveDate;

    @Column(name = "notes", length = 500)
    private String notes;

    public CareerHistory(Employee employee, Position position, PositionLevel positionLevel,
                          BigDecimal salary, ContractType contractType, CareerChangeReason reason,
                          LocalDate effectiveDate, String notes) {
        this.employee = employee;
        this.position = position;
        this.positionLevel = positionLevel;
        this.salary = salary;
        this.contractType = contractType;
        this.reason = reason;
        this.effectiveDate = effectiveDate;
        this.notes = notes;
    }
}
