package com.proautokimium.api.Infrastructure.repositories;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.proautokimium.api.domain.entities.FuelSupply;

public interface FuelSupplyRepository extends JpaRepository<FuelSupply, UUID>{


    List<FuelSupply> findByFuelSupplyDateBetween(LocalDate start, LocalDate end);
}
