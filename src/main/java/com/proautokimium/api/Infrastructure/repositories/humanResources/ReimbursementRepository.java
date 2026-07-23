package com.proautokimium.api.Infrastructure.repositories.humanResources;

import com.proautokimium.api.domain.entities.Employee;
import com.proautokimium.api.domain.entities.humanResources.Reimbursement;
import com.proautokimium.api.domain.enums.humanResources.ReimbursementStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface ReimbursementRepository extends JpaRepository<Reimbursement, UUID> {
    List<Reimbursement> findByEmployeeOrderByRequestedAtDesc(Employee employee);
    List<Reimbursement> findByStatusOrderByRequestedAtDesc(ReimbursementStatus status);
    List<Reimbursement> findAllByOrderByRequestedAtDesc();
}
