package com.proautokimium.api.Infrastructure.services;

import com.proautokimium.api.Application.DTOs.vehicle.RevisionRequestDTO;
import com.proautokimium.api.Application.DTOs.vehicle.VehicleRequestDTO;
import com.proautokimium.api.Infrastructure.repositories.RevisionRepository;
import com.proautokimium.api.Infrastructure.repositories.VehicleRepository;
import com.proautokimium.api.domain.entities.Revision;
import com.proautokimium.api.domain.entities.Vehicle;
import jakarta.transaction.Transactional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class VehicleService {
    private final VehicleRepository vehicleRepository;
    private final RevisionRepository revisionRepository;

    public VehicleService(VehicleRepository vehicleRepository, RevisionRepository revisionRepository) {
        this.vehicleRepository = vehicleRepository;
        this.revisionRepository = revisionRepository;
    }

    @Transactional
    public Vehicle saveVehicle(VehicleRequestDTO dto){
        Vehicle vehicle = new Vehicle();

        vehicle.setNome(dto.nome());
        vehicle.setPlaca(dto.placa());
        vehicle.setMarca(dto.marca());
        vehicle.setConsumoUrbanoAlcool(dto.consumoUrbanoAlcool());
        vehicle.setConsumoUrbanoGasolina(dto.consumoUrbanoGasolina());
        vehicle.setConsumoRodoviarioAlcool(dto.consumoRodoviarioAlcool());
        vehicle.setConsumoRodoviarioGasolina(dto.consumoRodoviarioGasolina());

        return vehicleRepository.save(vehicle);
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
        Vehicle vehicle = vehicleRepository.findVehicleByPlaca(plate);
        return vehicle;
    }

    @Transactional
    public Revision includeRevision(Vehicle vehicle, RevisionRequestDTO dto){
        Revision revision = new Revision();
        revision.setRevisionDate(dto.revisionDate());
        revision.setVehicle(vehicle);

        return revisionRepository.save(revision);
    }

    public Set<Revision> getRevisions(){
        return revisionRepository.findAll().stream().collect(Collectors.toSet());
    }
}
