package com.proautokimium.api.Infrastructure.repositories.prostock;

import com.proautokimium.api.domain.entities.prostock.machine.MachineMovement;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.UUID;

public interface MachineMovementRepository extends JpaRepository<MachineMovement, UUID> {
    @Query(value = "SELECT * FROM machine_movements WHERE machine_id = :id", nativeQuery = true)
    List<MachineMovement> findMovementsByMachineId(@Param("id") UUID id);
}
