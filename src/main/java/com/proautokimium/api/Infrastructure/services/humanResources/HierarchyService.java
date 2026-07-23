package com.proautokimium.api.Infrastructure.services.humanResources;

import com.proautokimium.api.Application.DTOs.humanResources.Hierarchy.CreateHierarchyRequestDTO;
import com.proautokimium.api.Application.DTOs.humanResources.Hierarchy.HierarchyResponseDTO;
import com.proautokimium.api.Infrastructure.repositories.humanResources.HierarchyRepository;
import com.proautokimium.api.domain.entities.humanResources.Hierarchy;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class HierarchyService {

    private final HierarchyRepository hierarchyRepository;

    public HierarchyService(HierarchyRepository hierarchyRepository) {
        this.hierarchyRepository = hierarchyRepository;
    }

    public HierarchyResponseDTO create(CreateHierarchyRequestDTO request){
        Hierarchy hierarchy = new Hierarchy(
                request.name(),
                request.levelOrder()
        );

        Hierarchy saved = hierarchyRepository.save(hierarchy);
        return toResponse(saved);
    }

    public List<HierarchyResponseDTO> listAll(){
        return hierarchyRepository.findAll().stream()
                .map(this::toResponse)
                .toList();
    }

    private HierarchyResponseDTO toResponse(Hierarchy hierarchy){
        return new HierarchyResponseDTO(
                hierarchy.getId(),
                hierarchy.getName(),
                hierarchy.getLevelOrder()
        );
    }
}
