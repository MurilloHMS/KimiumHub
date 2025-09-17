package com.proautokimium.api.Infrastructure.repositories;

import com.proautokimium.api.domain.entities.ServiceLocation;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface ServiceLocationRepository extends JpaRepository<ServiceLocation, UUID> {
    ServiceLocation findServiceLocationBySystemCode(String systemCode);
    ServiceLocation findServiceLocationByName(String name);
}
