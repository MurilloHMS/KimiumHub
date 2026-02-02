package com.proautokimium.api.Infrastructure.repositories;

import com.proautokimium.api.domain.entities.RegistroPonto;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface RegistroPontoRepository extends JpaRepository<RegistroPonto, UUID> {

    List<RegistroPonto> findByEmployee_IdOrderByDataDesc(UUID employeeId);
    List<RegistroPonto> findByEmployee_IdAndMesAnoOrderByDataDesc(UUID employeeId, String mesAno);
    Optional<RegistroPonto> findByEmployee_IdAndData(UUID employeeId, LocalDate data);

    @Query("SELECT r FROM RegistroPonto r WHERE r.employee.id = :employeeId AND r.data BETWEEN :startDate AND :endDate ORDER BY r.data DESC")
    List<RegistroPonto> findByIdAndPeriod(
            @Param("employeeId") UUID id,
            @Param("startDate")LocalDate startDate,
            @Param("endDate") LocalDate endDate
            );

    Long countById(UUID id);
    void deleteById(UUID id);
}
