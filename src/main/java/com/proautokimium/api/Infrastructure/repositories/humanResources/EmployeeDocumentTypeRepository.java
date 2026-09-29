package com.proautokimium.api.Infrastructure.repositories.humanResources;

import com.proautokimium.api.domain.entities.humanResources.EmployeeDocumentType;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface EmployeeDocumentTypeRepository extends JpaRepository<EmployeeDocumentType, UUID> {
    List<EmployeeDocumentType> findAllByOrderByNameAsc();

    /** "ASO" e "aso" são o mesmo tipo: a checagem ignora caixa antes do banco recusar. */
    boolean existsByNameIgnoreCase(String name);

    boolean existsByNameIgnoreCaseAndIdNot(String name, UUID id);
}
