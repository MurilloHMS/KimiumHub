package com.proautokimium.api.Infrastructure.repositories;

import com.proautokimium.api.domain.entities.Vehicle;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.ActiveProfiles;
import static org.assertj.core.api.Assertions.assertThat;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

@DataJpaTest
@ActiveProfiles("dev")
@AutoConfigureTestDatabase(
        replace = AutoConfigureTestDatabase.Replace.NONE
)
class VehicleRepositoryTest {
    @Autowired
    VehicleRepository vehicleRepository;

    @Autowired
    EntityManager entityManager;

    @Test
    @DisplayName("Should get vehicle successfully from plate")
    void findVehicleByPlaca() {
        java.util.Optional<Vehicle> result = Optional.ofNullable(this.vehicleRepository.findVehicleByPlaca("FFR-7092"));

        assertThat(result.isPresent()).isTrue();
    }

    @Test
    @DisplayName("Should not get vehicle from plate when vehicle not exists")
    void findVehicleByPlacaNotExists() {
        java.util.Optional<Vehicle> result = Optional.ofNullable(this.vehicleRepository.findVehicleByPlaca("FFR-7093"));

        assertThat(result.isEmpty()).isTrue();
    }
}