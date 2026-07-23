package com.proautokimium.api.Infrastructure.repositories.humanResources;

import com.proautokimium.api.domain.entities.Employee;
import com.proautokimium.api.domain.entities.humanResources.CareerHistory;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface CareerHistoryRepository extends JpaRepository<CareerHistory, UUID> {
    List<CareerHistory> findByEmployeeOrderByEffectiveDateDesc(Employee employee);
}
