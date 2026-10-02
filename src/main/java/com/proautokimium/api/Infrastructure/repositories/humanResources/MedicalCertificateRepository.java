package com.proautokimium.api.Infrastructure.repositories.humanResources;

import com.proautokimium.api.domain.entities.Employee;
import com.proautokimium.api.domain.entities.humanResources.MedicalCertificate;
import com.proautokimium.api.domain.enums.humanResources.MedicalCertificateStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public interface MedicalCertificateRepository extends JpaRepository<MedicalCertificate, UUID> {
    List<MedicalCertificate> findByEmployeeOrderByStartDateDesc(Employee employee);
    List<MedicalCertificate> findAllByOrderBySubmittedAtDesc();
    List<MedicalCertificate> findByStatusOrderBySubmittedAtDesc(MedicalCertificateStatus status);
    /** Recusado não conta: não é afastamento que o RH aceitou. */
    long countByEmployeeAndStatusNotAndStartDateBetween(Employee employee, MedicalCertificateStatus status,
                                                        LocalDate rangeStart, LocalDate rangeEnd);
}
