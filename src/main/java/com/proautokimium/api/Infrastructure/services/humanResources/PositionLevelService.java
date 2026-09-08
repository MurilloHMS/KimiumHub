package com.proautokimium.api.Infrastructure.services.humanResources;

import com.proautokimium.api.Application.DTOs.humanResources.PositionLevel.CreatePositionLevelRequestDTO;
import com.proautokimium.api.Application.DTOs.humanResources.PositionLevel.PositionLevelResponseDTO;
import com.proautokimium.api.Infrastructure.exceptions.humanResources.PositionLevelNotFoundException;
import com.proautokimium.api.Infrastructure.exceptions.humanResources.PositionNotFoundException;
import com.proautokimium.api.Infrastructure.repositories.humanResources.PositionLevelRepository;
import com.proautokimium.api.Infrastructure.repositories.humanResources.PositionRepository;
import com.proautokimium.api.domain.entities.humanResources.Position;
import com.proautokimium.api.domain.entities.humanResources.PositionLevel;
import com.proautokimium.api.domain.enums.humanResources.SalaryAdjustmentType;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@Service
public class PositionLevelService {

    private final PositionRepository positionRepository;
    private final PositionLevelRepository positionLevelRepository;
    private final PositionLevelSalaryResolver salaryResolver;

    public PositionLevelService(
            PositionRepository positionRepository,
            PositionLevelRepository positionLevelRepository,
            PositionLevelSalaryResolver salaryResolver
    ) {
        this.positionRepository = positionRepository;
        this.positionLevelRepository = positionLevelRepository;
        this.salaryResolver = salaryResolver;
    }

    public PositionLevelResponseDTO create(CreatePositionLevelRequestDTO request) {
        Position position = positionRepository.findById(request.positionId())
                .orElseThrow(PositionNotFoundException::new);

        PositionLevel level = request.adjustmentType() == SalaryAdjustmentType.FIXED
                ? PositionLevel.fixed(request.name(), request.levelOrder(), position, request.fixedAmount())
                : PositionLevel.percentage(request.name(), request.levelOrder(), position, request.percentageIncrease());

        PositionLevel saved = positionLevelRepository.save(level);
        return toResponse(saved);
    }

    /**
     * Altera o nivel — e e por aqui que o valor base muda.
     *
     * A resposta traz o `resolvedSalary` recalculado deste nivel. **Os outros
     * niveis do cargo tambem mudam**: nivel PERCENTAGE resolve sobre o
     * imediatamente anterior, entao mexer no valor base do primeiro reprecifica
     * a escala inteira. Quem chama precisa reler a lista do cargo; a tela ja
     * faz isso.
     */
    public PositionLevelResponseDTO update(UUID id, CreatePositionLevelRequestDTO request) {
        PositionLevel level = positionLevelRepository.findById(id)
                .orElseThrow(PositionLevelNotFoundException::new);

        level.alterar(
                request.name(),
                request.levelOrder(),
                request.adjustmentType(),
                request.fixedAmount(),
                request.percentageIncrease());

        return toResponse(positionLevelRepository.save(level));
    }

    public List<PositionLevelResponseDTO> listByPosition(UUID positionId) {
        Position position = positionRepository.findById(positionId)
                .orElseThrow(PositionNotFoundException::new);

        return positionLevelRepository.findByPositionOrderByLevelOrderAsc(position).stream()
                .map(this::toResponse)
                .toList();
    }

    private PositionLevelResponseDTO toResponse(PositionLevel level) {
        BigDecimal resolvedSalary = salaryResolver.resolve(level);
        return new PositionLevelResponseDTO(
                level.getId(),
                level.getName(),
                level.getLevelOrder(),
                level.getPosition().getId(),
                level.getAdjustmentType(),
                level.getFixedAmount(),
                level.getPercentageIncrease(),
                resolvedSalary
        );
    }
}
