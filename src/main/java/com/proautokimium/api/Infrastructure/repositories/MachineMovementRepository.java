package com.proautokimium.api.Infrastructure.repositories;

import com.proautokimium.api.domain.entities.MovementMachine;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.UUID;

public interface MachineMovementRepository extends JpaRepository<MovementMachine, UUID> {
    @Query(value = "SELECT * FROM machine_movements WHERE machine_id = :id", nativeQuery = true)
    List<MovementMachine> findMovementsByMachineId(@Param("id") UUID id);
}
