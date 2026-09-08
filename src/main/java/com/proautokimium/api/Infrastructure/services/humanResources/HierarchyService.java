package com.proautokimium.api.Infrastructure.services.humanResources;

import com.proautokimium.api.Application.DTOs.humanResources.Hierarchy.CreateHierarchyRequestDTO;
import com.proautokimium.api.Application.DTOs.humanResources.Hierarchy.HierarchyResponseDTO;
import com.proautokimium.api.Infrastructure.exceptions.humanResources.CadastroEmUsoException;
import com.proautokimium.api.Infrastructure.exceptions.humanResources.HierarchyNotFoundException;
import com.proautokimium.api.Infrastructure.repositories.EmployeeRepository;
import com.proautokimium.api.Infrastructure.repositories.humanResources.HierarchyRepository;
import com.proautokimium.api.domain.entities.humanResources.Hierarchy;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Service
public class HierarchyService {

    private final HierarchyRepository hierarchyRepository;
    private final EmployeeRepository employeeRepository;

    public HierarchyService(
            HierarchyRepository hierarchyRepository,
            EmployeeRepository employeeRepository
    ) {
        this.hierarchyRepository = hierarchyRepository;
        this.employeeRepository = employeeRepository;
    }

    public HierarchyResponseDTO create(CreateHierarchyRequestDTO request){
        Hierarchy hierarchy = new Hierarchy(
                request.name(),
                request.levelOrder()
        );

        Hierarchy saved = hierarchyRepository.save(hierarchy);
        return toResponse(saved);
    }

    public HierarchyResponseDTO update(UUID id, CreateHierarchyRequestDTO request){
        Hierarchy hierarchy = hierarchyRepository.findById(id)
                .orElseThrow(HierarchyNotFoundException::new);

        hierarchy.setName(request.name());
        hierarchy.setLevelOrder(request.levelOrder());

        return toResponse(hierarchyRepository.save(hierarchy));
    }

    public void delete(UUID id){
        Hierarchy hierarchy = hierarchyRepository.findById(id)
                .orElseThrow(HierarchyNotFoundException::new);

        long funcionarios = employeeRepository.countByHierarquiaId(id);

        if (funcionarios > 0) {
            throw new CadastroEmUsoException(
                    "Nao da para excluir: " + funcionarios
                    + (funcionarios == 1 ? " funcionario esta" : " funcionarios estao")
                    + " nesta hierarquia.");
        }

        hierarchyRepository.delete(hierarchy);
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
