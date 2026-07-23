package com.proautokimium.api.Infrastructure.services.humanResources;

import com.proautokimium.api.domain.entities.humanResources.PositionLevel;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

@Component
public class PercentageSalaryStrategy implements SalaryCalculationStrategy {
    @Override
    public BigDecimal calculate(PositionLevel level, BigDecimal previousLevelSalary) {
        BigDecimal multiplier = BigDecimal.ONE.add(
                level.getPercentageIncrease().divide(BigDecimal.valueOf(100))
        );
        return previousLevelSalary.multiply(multiplier);
    }
}
