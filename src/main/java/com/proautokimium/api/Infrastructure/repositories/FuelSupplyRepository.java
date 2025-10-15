package com.proautokimium.api.Infrastructure.repositories;

import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.proautokimium.api.domain.entities.FuelSupply;

public interface FuelSupplyRepository extends JpaRepository<FuelSupply, UUID>{

}
