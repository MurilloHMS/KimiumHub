package com.proautokimium.api.Infrastructure.repositories.prostock;

import com.proautokimium.api.domain.entities.prostock.machine.MachineScheduleChange;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface MachineScheduleChangeRepository extends JpaRepository<MachineScheduleChange, UUID> {

    /** Mais recente primeiro — a última justificativa é a que se lê antes. */
    List<MachineScheduleChange> findByRegisterIdOrderByChangedAtDesc(UUID registerId);
}
