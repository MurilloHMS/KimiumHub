package com.proautokimium.api.Infrastructure.repositories.humanResources;

import com.proautokimium.api.domain.entities.Employee;
import com.proautokimium.api.domain.entities.humanResources.CareerHistory;
import com.proautokimium.api.domain.entities.humanResources.Position;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.UUID;

public interface CareerHistoryRepository extends JpaRepository<CareerHistory, UUID> {
    List<CareerHistory> findByEmployeeOrderByEffectiveDateDesc(Employee employee);

    /** Snapshot mais recente de cada funcionário (qualquer posição) — usado pro dissídio ALL_POSITIONS. */
    @Query("""
            SELECT ch FROM CareerHistory ch
            WHERE ch.effectiveDate = (
                SELECT MAX(ch2.effectiveDate) FROM CareerHistory ch2 WHERE ch2.employee = ch.employee
            )
            """)
    List<CareerHistory> findLatestPerEmployee();

    /** Snapshot mais recente de cada funcionário, restrito a quem está atualmente num Position — dissídio SPECIFIC_POSITION. */
    @Query("""
            SELECT ch FROM CareerHistory ch
            WHERE ch.position = :position
            AND ch.effectiveDate = (
                SELECT MAX(ch2.effectiveDate) FROM CareerHistory ch2 WHERE ch2.employee = ch.employee
            )
            """)
    List<CareerHistory> findLatestPerEmployeeByPosition(@Param("position") Position position);
}
