package com.proautokimium.api.domain.entities.humanResources;

import com.proautokimium.api.domain.enums.humanResources.SalaryAdjustmentType;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@jakarta.persistence.Entity
@Table(name = "position_levels")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class PositionLevel extends com.proautokimium.api.domain.abstractions.Entity {

    @Column(name = "name", length = 50, nullable = false)
    private String name;

    @Column(name = "level_order", nullable = false)
    private Integer levelOrder;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "position_id", nullable = false)
    private Position position;

    @Enumerated(EnumType.STRING)
    @Column(name = "adjustment_type", nullable = false, length = 20)
    private SalaryAdjustmentType adjustmentType;

    @Column(name = "fixed_amount", precision = 10, scale = 2)
    private BigDecimal fixedAmount;

    @Column(name = "percentage_increase", precision = 5, scale = 2)
    private BigDecimal percentageIncrease;

    private PositionLevel(String name, Integer levelOrder, Position position,
                           SalaryAdjustmentType adjustmentType,
                           BigDecimal fixedAmount, BigDecimal percentageIncrease) {
        this.name = name;
        this.levelOrder = levelOrder;
        this.position = position;
        this.adjustmentType = adjustmentType;
        this.fixedAmount = fixedAmount;
        this.percentageIncrease = percentageIncrease;
    }

    public static PositionLevel fixed(String name, Integer levelOrder, Position position, BigDecimal fixedAmount) {
        return new PositionLevel(name, levelOrder, position, SalaryAdjustmentType.FIXED, fixedAmount, null);
    }

    public static PositionLevel percentage(String name, Integer levelOrder, Position position, BigDecimal percentageIncrease) {
        return new PositionLevel(name, levelOrder, position, SalaryAdjustmentType.PERCENTAGE, null, percentageIncrease);
    }
}
