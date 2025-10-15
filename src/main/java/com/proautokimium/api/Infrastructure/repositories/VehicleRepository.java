package com.proautokimium.api.Infrastructure.repositories;

import com.proautokimium.api.domain.entities.Vehicle;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.UUID;

public interface VehicleRepository extends JpaRepository<Vehicle, UUID> {
    Vehicle findVehicleByPlaca(String placa);
}
