package com.proautokimium.api.Infrastructure.services.humanResources;

import com.proautokimium.api.domain.entities.humanResources.PositionLevel;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

@Component
public class FixedSalaryStrategy implements SalaryCalculationStrategy {
    @Override
    public BigDecimal calculate(PositionLevel level, BigDecimal previousLevelSalary) {
        return level.getFixedAmount();
    }
}
