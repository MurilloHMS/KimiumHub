package com.proautokimium.api.Infrastructure.repositories.prostock;

import com.proautokimium.api.domain.entities.prostock.machine.MachineScheduleChange;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public interface MachineScheduleChangeRepository extends JpaRepository<MachineScheduleChange, UUID> {

    /** Mais recente primeiro — a última justificativa é a que se lê antes. */
    List<MachineScheduleChange> findByRegisterIdOrderByChangedAtDesc(UUID registerId);

    /**
     * Os adiamentos de um período, com a programação e a máquina já juntas.
     *
     * `JOIN FETCH` de propósito: o Hub mostra cliente e máquina em cada linha, e
     * sem isso seria um SELECT por adiamento para carregar o LAZY.
     */
    @Query("""
        SELECT c
        FROM MachineScheduleChange c
        JOIN FETCH c.register r
        JOIN FETCH r.machine
        WHERE c.changedAt >= :from
        ORDER BY c.changedAt DESC
        """)
    List<MachineScheduleChange> findSince(@Param("from") LocalDateTime from);
}
