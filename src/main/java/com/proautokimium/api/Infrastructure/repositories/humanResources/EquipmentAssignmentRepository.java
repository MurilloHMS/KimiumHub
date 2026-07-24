package com.proautokimium.api.Infrastructure.repositories.humanResources;

import com.proautokimium.api.domain.entities.Employee;
import com.proautokimium.api.domain.entities.humanResources.EquipmentAssignment;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface EquipmentAssignmentRepository extends JpaRepository<EquipmentAssignment, UUID> {
    List<EquipmentAssignment> findByEmployeeOrderByDeliveredAtDesc(Employee employee);
    List<EquipmentAssignment> findByReturnedAtIsNull();
}
