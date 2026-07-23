package com.proautokimium.api.Infrastructure.services.humanResources;

import com.proautokimium.api.domain.entities.humanResources.PositionLevel;

import java.math.BigDecimal;

public interface SalaryCalculationStrategy {
    BigDecimal calculate(PositionLevel level, BigDecimal previousLevelSalary);
}
