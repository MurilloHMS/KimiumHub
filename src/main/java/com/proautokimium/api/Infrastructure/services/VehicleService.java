package com.proautokimium.api.Infrastructure.services;

import com.proautokimium.api.Application.DTOs.vehicle.RevisionRequestDTO;
import com.proautokimium.api.Application.DTOs.vehicle.VehicleRequestDTO;
import com.proautokimium.api.Infrastructure.repositories.RevisionRepository;
import com.proautokimium.api.Infrastructure.repositories.ServiceLocationRepository;
import com.proautokimium.api.Infrastructure.repositories.VehicleRepository;
import com.proautokimium.api.domain.entities.Revision;
import com.proautokimium.api.domain.entities.ServiceLocation;
import com.proautokimium.api.domain.entities.Vehicle;
import jakarta.transaction.Transactional;
import org.springframework.stereotype.Service;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class VehicleService {

    private final VehicleRepository vehicleRepository;
    private final RevisionRepository revisionRepository;
    private final ServiceLocationRepository serviceLocationRepository;

    public VehicleService(VehicleRepository vehicleRepository, RevisionRepository revisionRepository, ServiceLocationRepository serviceLocationRepository) {
        this.serviceLocationRepository = serviceLocationRepository;
        this.vehicleRepository = vehicleRepository;
        this.revisionRepository = revisionRepository;
    }

    @Transactional
    public void saveVehicle(VehicleRequestDTO dto){
        Vehicle vehicle = new Vehicle();

        vehicle.setNome(dto.nome());
        vehicle.setPlaca(dto.placa());
        vehicle.setMarca(dto.marca());
        vehicle.setConsumoUrbanoAlcool(dto.consumoUrbanoAlcool());
        vehicle.setConsumoUrbanoGasolina(dto.consumoUrbanoGasolina());
        vehicle.setConsumoRodoviarioAlcool(dto.consumoRodoviarioAlcool());
        vehicle.setConsumoRodoviarioGasolina(dto.consumoRodoviarioGasolina());

        vehicleRepository.save(vehicle);
    }

    @Transactional
    public Vehicle addRevisionsToVehicle(UUID vehicleId, Set<UUID> revisionsIds){
        Vehicle vehicle = vehicleRepository.findById(vehicleId).orElseThrow(
                () -> new RuntimeException("Vehicle Not Found"));

        Set<Revision> revisions = revisionRepository.findAllById(revisionsIds)
                .stream()
                .peek(r -> r.setVehicle(vehicle))
                .collect(Collectors.toSet());

        vehicle.getRevisions().addAll(revisions);
        return vehicleRepository.save(vehicle);
    }

    @Transactional
    public Vehicle getVehicleByPlate(String plate){
        return vehicleRepository.findVehicleByPlaca(plate);
    }

    @Transactional
    public void includeRevision(Vehicle vehicle, RevisionRequestDTO dto){
        Revision revision = new Revision();
        revision.setRevisionDate(dto.revisionDate());
        revision.setVehicle(vehicle);
        revision.setDriver(dto.driver());
        revision.setFiscalNote(dto.nfe());
        revision.setKilometer(dto.kilometer());
        revision.setType(dto.type());
        revision.setObservation(dto.observation());
        

        ServiceLocation location = serviceLocationRepository.findServiceLocationByCodParceiro(dto.localSystemCode());
        revision.setLocal(location);

        revisionRepository.save(revision);
    }

    public Set<Revision> getRevisions(){
        return new HashSet<>(revisionRepository.findAll());
    }
}
