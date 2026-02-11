package com.proautokimium.api.Infrastructure.repositories;

import com.proautokimium.api.domain.entities.ProductMachine;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface MachineRepository extends JpaRepository<ProductMachine, UUID> {
}
