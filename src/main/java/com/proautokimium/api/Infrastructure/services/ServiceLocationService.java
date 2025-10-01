package com.proautokimium.api.Infrastructure.services;

import com.proautokimium.api.Application.DTOs.partners.ServiceLocationDTO;
import com.proautokimium.api.Infrastructure.repositories.ServiceLocationRepository;
import com.proautokimium.api.domain.entities.ServiceLocation;
import com.proautokimium.api.domain.valueObjects.Email;
import jakarta.transaction.Transactional;
import org.springframework.stereotype.Service;

import java.util.List;

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

    public List<ServiceLocation> getAllServiceLocations() {
        return repository.findAll();
    }

    public void updateServiceLocation(ServiceLocationDTO dto){
        var sl = repository.findServiceLocationByCodParceiro(dto.codParceiro());

        sl.setAddress(dto.address());
        sl.setAtivo(dto.ativo());
        sl.setDocumento(dto.documento());
        sl.setName(dto.nome());
        sl.setEmail(new Email(dto.email()));

        repository.save(sl);
    }

}
