package com.proautokimium.api.Infrastructure.services.humanResources;

import com.proautokimium.api.Infrastructure.exceptions.humanResources.PositionLevelNotFoundException;
import com.proautokimium.api.Infrastructure.repositories.humanResources.PositionLevelRepository;
import com.proautokimium.api.domain.entities.humanResources.PositionLevel;
import com.proautokimium.api.domain.enums.humanResources.SalaryAdjustmentType;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.Map;

@Service
public class PositionLevelSalaryResolver {

    private final PositionLevelRepository positionLevelRepository;
    private final Map<SalaryAdjustmentType, SalaryCalculationStrategy> strategies;

    public PositionLevelSalaryResolver(
            PositionLevelRepository positionLevelRepository,
            FixedSalaryStrategy fixedSalaryStrategy,
            PercentageSalaryStrategy percentageSalaryStrategy
    ) {
        this.positionLevelRepository = positionLevelRepository;
        this.strategies = Map.of(
                SalaryAdjustmentType.FIXED, fixedSalaryStrategy,
                SalaryAdjustmentType.PERCENTAGE, percentageSalaryStrategy
        );
    }

    public BigDecimal resolve(PositionLevel level) {
        SalaryCalculationStrategy strategy = strategies.get(level.getAdjustmentType());

        if (level.getAdjustmentType() == SalaryAdjustmentType.FIXED) {
            return strategy.calculate(level, null);
        }

        PositionLevel previousLevel = positionLevelRepository
                .findByPositionAndLevelOrder(level.getPosition(), level.getLevelOrder() - 1)
                .orElseThrow(PositionLevelNotFoundException::new);

        BigDecimal previousLevelSalary = resolve(previousLevel);
        return strategy.calculate(level, previousLevelSalary);
    }
}
