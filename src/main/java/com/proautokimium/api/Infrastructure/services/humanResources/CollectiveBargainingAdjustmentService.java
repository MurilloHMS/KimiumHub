package com.proautokimium.api.Infrastructure.services.humanResources;

import com.proautokimium.api.Application.DTOs.humanResources.CollectiveBargainingAdjustment.ApplyCollectiveBargainingAdjustmentRequestDTO;
import com.proautokimium.api.Application.DTOs.humanResources.CollectiveBargainingAdjustment.CollectiveBargainingAdjustmentResponseDTO;
import com.proautokimium.api.Infrastructure.exceptions.humanResources.PositionNotFoundException;
import com.proautokimium.api.Infrastructure.repositories.humanResources.CareerHistoryRepository;
import com.proautokimium.api.Infrastructure.repositories.humanResources.CollectiveBargainingAdjustmentRepository;
import com.proautokimium.api.Infrastructure.repositories.humanResources.PositionLevelRepository;
import com.proautokimium.api.Infrastructure.repositories.humanResources.PositionRepository;
import com.proautokimium.api.domain.entities.humanResources.CareerHistory;
import com.proautokimium.api.domain.entities.humanResources.CollectiveBargainingAdjustment;
import com.proautokimium.api.domain.entities.humanResources.Position;
import com.proautokimium.api.domain.entities.humanResources.PositionLevel;
import com.proautokimium.api.domain.enums.humanResources.AdjustmentScope;
import com.proautokimium.api.domain.enums.humanResources.CareerChangeReason;
import com.proautokimium.api.domain.enums.humanResources.SalaryAdjustmentType;
import jakarta.transaction.Transactional;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;

@Service
public class CollectiveBargainingAdjustmentService {

    private final CollectiveBargainingAdjustmentRepository adjustmentRepository;
    private final PositionRepository positionRepository;
    private final PositionLevelRepository positionLevelRepository;
    private final CareerHistoryRepository careerHistoryRepository;
    private final PositionLevelSalaryResolver salaryResolver;

    public CollectiveBargainingAdjustmentService(
            CollectiveBargainingAdjustmentRepository adjustmentRepository,
            PositionRepository positionRepository,
            PositionLevelRepository positionLevelRepository,
            CareerHistoryRepository careerHistoryRepository,
            PositionLevelSalaryResolver salaryResolver
    ) {
        this.adjustmentRepository = adjustmentRepository;
        this.positionRepository = positionRepository;
        this.positionLevelRepository = positionLevelRepository;
        this.careerHistoryRepository = careerHistoryRepository;
        this.salaryResolver = salaryResolver;
    }

    /**
     * Aplica o dissídio: reajusta os níveis FIXED no escopo (níveis PERCENTAGE
     * se ajustam sozinhos, pois são relativos) e gera um novo CareerHistory
     * (motivo COLLECTIVE_BARGAINING_ADJUSTMENT) pro snapshot mais recente de
     * cada funcionário afetado.
     */
    @Transactional
    public CollectiveBargainingAdjustmentResponseDTO apply(ApplyCollectiveBargainingAdjustmentRequestDTO request) {
        CollectiveBargainingAdjustment adjustment;
        List<PositionLevel> fixedLevels;
        List<CareerHistory> latestSnapshots;

        if (request.scope() == AdjustmentScope.ALL_POSITIONS) {
            adjustment = CollectiveBargainingAdjustment.allPositions(request.percentage(), request.effectiveDate());
            fixedLevels = positionLevelRepository.findByAdjustmentType(SalaryAdjustmentType.FIXED);
            latestSnapshots = careerHistoryRepository.findLatestPerEmployee();
        } else {
            Position position = positionRepository.findById(request.positionId())
                    .orElseThrow(PositionNotFoundException::new);
            adjustment = CollectiveBargainingAdjustment.specificPosition(request.percentage(), request.effectiveDate(), position);
            fixedLevels = positionLevelRepository.findByPositionAndAdjustmentType(position, SalaryAdjustmentType.FIXED);
            latestSnapshots = careerHistoryRepository.findLatestPerEmployeeByPosition(position);
        }

        adjustmentRepository.save(adjustment);

        BigDecimal multiplier = BigDecimal.ONE.add(request.percentage().divide(BigDecimal.valueOf(100)));
        for (PositionLevel level : fixedLevels) {
            level.applyFixedAmountIncrease(multiplier);
        }
        positionLevelRepository.saveAll(fixedLevels);

        for (CareerHistory snapshot : latestSnapshots) {
            BigDecimal newSalary = salaryResolver.resolve(snapshot.getPositionLevel());

            CareerHistory newSnapshot = new CareerHistory(
                    snapshot.getEmployee(),
                    snapshot.getPosition(),
                    snapshot.getPositionLevel(),
                    newSalary,
                    snapshot.getContractType(),
                    CareerChangeReason.COLLECTIVE_BARGAINING_ADJUSTMENT,
                    request.effectiveDate(),
                    "Dissídio de " + request.percentage() + "%"
            );
            careerHistoryRepository.save(newSnapshot);
        }

        return toResponse(adjustment, fixedLevels.size(), latestSnapshots.size());
    }

    private CollectiveBargainingAdjustmentResponseDTO toResponse(
            CollectiveBargainingAdjustment adjustment, int levelsUpdated, int employeesAffected) {
        return new CollectiveBargainingAdjustmentResponseDTO(
                adjustment.getId(),
                adjustment.getPercentage(),
                adjustment.getEffectiveDate(),
                adjustment.getScope(),
                adjustment.getPosition() != null ? adjustment.getPosition().getId() : null,
                levelsUpdated,
                employeesAffected
        );
    }
}
