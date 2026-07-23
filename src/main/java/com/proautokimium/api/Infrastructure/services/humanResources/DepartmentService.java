package com.proautokimium.api.Infrastructure.services.humanResources;

import com.proautokimium.api.Application.DTOs.humanResources.Department.CreateDepartmentRequestDTO;
import com.proautokimium.api.Application.DTOs.humanResources.Department.DepartmentResponseDTO;
import com.proautokimium.api.Infrastructure.repositories.humanResources.DepartmentRepository;
import com.proautokimium.api.domain.entities.humanResources.Department;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class DepartmentService {

    private final DepartmentRepository departmentRepository;

    public DepartmentService(DepartmentRepository departmentRepository) {
        this.departmentRepository = departmentRepository;
    }

    public DepartmentResponseDTO create(CreateDepartmentRequestDTO request){
        Department department = new Department();
        department.setName(request.name());
        Department saved = departmentRepository.save(department);
        return toResponse(saved);
    }

    public List<DepartmentResponseDTO> listAll(){
        return departmentRepository.findAll().stream()
                .map(this::toResponse)
                .toList();
    }

    private DepartmentResponseDTO toResponse(Department department){
        return new DepartmentResponseDTO(department.getId(), department.getName());
    }
}
