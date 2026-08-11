package com.proautokimium.api.Infrastructure.repositories.prostock;

import com.proautokimium.api.domain.entities.prostock.machine.MachineAlertConfig;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface MachineAlertConfigRepository extends JpaRepository<MachineAlertConfig, UUID> {
}
