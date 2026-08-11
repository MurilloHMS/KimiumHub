package com.proautokimium.api.Infrastructure.repositories.prostock;

import com.proautokimium.api.domain.entities.prostock.machine.MachineAlertSent;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.UUID;

public interface MachineAlertSentRepository extends JpaRepository<MachineAlertSent, UUID> {
    @Query("""
           SELECT COUNT(a) > 0 FROM MachineAlertSent a
           WHERE a.registerId = :registerId
             AND a.alertDate = :alertDate
             AND a.daysBefore = :daysBefore
           """)
    boolean alreadySent(@Param("registerId") UUID registerId,
                        @Param("alertDate") LocalDate alertDate,
                        @Param("daysBefore") int daysBefore);
}
