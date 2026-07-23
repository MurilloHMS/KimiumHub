package com.proautokimium.api.domain.entities.humanResources;

import com.proautokimium.api.domain.enums.humanResources.AdjustmentScope;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;

@jakarta.persistence.Entity
@Table(name = "collective_bargaining_adjustments")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class CollectiveBargainingAdjustment extends com.proautokimium.api.domain.abstractions.Entity {

    @Column(name = "percentage", nullable = false, precision = 5, scale = 2)
    private BigDecimal percentage;

    @Column(name = "effective_date", nullable = false)
    private LocalDate effectiveDate;

    @Enumerated(EnumType.STRING)
    @Column(name = "scope", nullable = false, length = 20)
    private AdjustmentScope scope;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "position_id")
    private Position position;

    private CollectiveBargainingAdjustment(BigDecimal percentage, LocalDate effectiveDate,
                                            AdjustmentScope scope, Position position) {
        this.percentage = percentage;
        this.effectiveDate = effectiveDate;
        this.scope = scope;
        this.position = position;
    }

    public static CollectiveBargainingAdjustment allPositions(BigDecimal percentage, LocalDate effectiveDate) {
        return new CollectiveBargainingAdjustment(percentage, effectiveDate, AdjustmentScope.ALL_POSITIONS, null);
    }

    public static CollectiveBargainingAdjustment specificPosition(BigDecimal percentage, LocalDate effectiveDate, Position position) {
        if (position == null) {
            throw new IllegalArgumentException("SPECIFIC_POSITION scope requires a Position");
        }
        return new CollectiveBargainingAdjustment(percentage, effectiveDate, AdjustmentScope.SPECIFIC_POSITION, position);
    }
}
