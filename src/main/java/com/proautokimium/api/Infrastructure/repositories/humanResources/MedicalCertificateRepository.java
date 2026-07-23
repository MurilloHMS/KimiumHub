package com.proautokimium.api.Infrastructure.repositories.humanResources;

import com.proautokimium.api.domain.entities.Employee;
import com.proautokimium.api.domain.entities.humanResources.MedicalCertificate;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public interface MedicalCertificateRepository extends JpaRepository<MedicalCertificate, UUID> {
    List<MedicalCertificate> findByEmployeeOrderByStartDateDesc(Employee employee);
    long countByEmployeeAndStartDateBetween(Employee employee, LocalDate rangeStart, LocalDate rangeEnd);
}
