package com.proautokimium.api.Infrastructure.repositories;

import com.proautokimium.api.domain.entities.Cliente;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface ClienteRepository extends JpaRepository<Cliente, UUID> {
}
