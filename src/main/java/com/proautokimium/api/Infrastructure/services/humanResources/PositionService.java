package com.proautokimium.api.Infrastructure.services.humanResources;

import com.proautokimium.api.Application.DTOs.humanResources.Position.CreatePositionRequestDTO;
import com.proautokimium.api.Application.DTOs.humanResources.Position.PositionResponseDTO;
import com.proautokimium.api.Infrastructure.repositories.humanResources.PositionRepository;
import com.proautokimium.api.domain.entities.humanResources.Position;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class PositionService {

    private final PositionRepository positionRepository;

    public PositionService(PositionRepository positionRepository) {
        this.positionRepository = positionRepository;
    }

    public PositionResponseDTO create(CreatePositionRequestDTO request) {
        Position position = new Position();
        position.setName(request.name());
        Position saved = positionRepository.save(position);
        return toResponse(saved);
    }

    public List<PositionResponseDTO> listAll() {
        return positionRepository.findAll().stream()
                .map(this::toResponse)
                .toList();
    }

    private PositionResponseDTO toResponse(Position position) {
        return new PositionResponseDTO(position.getId(), position.getName());
    }
}
