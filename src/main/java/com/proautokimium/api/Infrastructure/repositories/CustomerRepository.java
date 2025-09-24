package com.proautokimium.api.Infrastructure.repositories;

import com.proautokimium.api.domain.entities.Customer;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface CustomerRepository extends JpaRepository<Customer, UUID> {
    Customer findByCodParceiro(String codParceiro);
}
