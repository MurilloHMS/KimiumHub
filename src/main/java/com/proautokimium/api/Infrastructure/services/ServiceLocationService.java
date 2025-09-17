package com.proautokimium.api.Infrastructure.services;

import com.proautokimium.api.Infrastructure.repositories.ServiceLocationRepository;
import com.proautokimium.api.domain.entities.ServiceLocation;
import jakarta.transaction.Transactional;
import org.springframework.stereotype.Service;

@Service
public class ServiceLocationService {

    private final ServiceLocationRepository repository;

    public ServiceLocationService(ServiceLocationRepository repository) {
        this.repository = repository;
    }

    @Transactional
    public void saveServiceLocation(String name) {
        var location = repository.findServiceLocationByName(name);
        if (location == null) {
            var newLocation = new ServiceLocation();
            newLocation.setName(name);
            repository.save(newLocation);
        }
    }

    public ServiceLocation getServiceLocationByName(String name) {
        return repository.findServiceLocationByName(name);
    }
}
