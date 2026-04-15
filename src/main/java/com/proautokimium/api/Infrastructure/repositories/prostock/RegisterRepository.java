package com.proautokimium.api.Infrastructure.repositories.prostock;

import com.proautokimium.api.domain.entities.prostock.machine.Machine;
import com.proautokimium.api.domain.entities.prostock.machine.MachineRegister;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Arrays;
import java.util.List;
import java.util.UUID;

public interface RegisterRepository extends JpaRepository<MachineRegister, UUID> {
    List<MachineRegister> findAllByMachine(Machine machine);
}
