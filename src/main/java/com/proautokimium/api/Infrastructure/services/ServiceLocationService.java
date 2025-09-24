package com.proautokimium.api.Infrastructure.services;

import com.proautokimium.api.Application.DTOs.partners.ServiceLocationDTO;
import com.proautokimium.api.Infrastructure.repositories.ServiceLocationRepository;
import com.proautokimium.api.domain.entities.ServiceLocation;
import com.proautokimium.api.domain.valueObjects.Email;
import jakarta.transaction.Transactional;
import org.springframework.stereotype.Service;

@Service
public class ServiceLocationService {

    private final ServiceLocationRepository repository;

    public ServiceLocationService(ServiceLocationRepository repository) {
        this.repository = repository;
    }

    @Transactional
    public void createServiceLocation(ServiceLocationDTO dto) {
        ServiceLocation location = new ServiceLocation();

        location.setName(dto.nome());
        location.setAddress(dto.address());
        location.setAtivo(dto.ativo());
        location.setEmail(new Email(dto.email()));
        location.setDocumento(dto.documento());
        location.setCodParceiro(dto.codParceiro());

        repository.save(location);
    }

    public ServiceLocation getServiceLocationBySystemCode(String systemCode) {
        return repository.findServiceLocationByCodParceiro(systemCode);
    }

    @Transactional
    public void deleteServiceLocationBySystemCode(String systemCode) {
        var location = repository.findServiceLocationByCodParceiro(systemCode);
        if (location != null) {
            repository.delete(location);
        }
    }

    public Iterable<ServiceLocation> getAllServiceLocations() {
        return repository.findAll();
    }

}
