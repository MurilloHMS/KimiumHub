package com.proautokimium.api.controllers;

import com.proautokimium.api.Application.DTOs.vehicle.RevisionRequestDTO;
import com.proautokimium.api.Infrastructure.services.ServiceLocationService;
import com.proautokimium.api.Infrastructure.services.VehicleService;
import com.proautokimium.api.domain.entities.Vehicle;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("api/vehicle/revision")
public class RevisionController {

    @Autowired
    VehicleService service;

    @Autowired
    ServiceLocationService serviceLocationService;

    @PostMapping
    public ResponseEntity<Object> addRevision(@RequestBody @NotNull @Valid RevisionRequestDTO dto){
        Vehicle vehicle = service.getVehicleByPlate(dto.vehiclePlate());
        service.includeRevision(vehicle, dto);
        return ResponseEntity.ok().build();
    }

    @GetMapping
    public ResponseEntity<Object> GetRevisions(){
        var revisions = service.getRevisions().stream().map(r -> new RevisionRequestDTO(
        		r.getRevisionDate(),
        		r.getVehicle().getPlaca(),
        		r.getKilometer(),
        		r.getFiscalNote(),
        		r.getType(),
        		r.getDriver(),
        		r.getObservation(),
        		r.getLocal().getCodParceiro().toString())).toList();
        
        return revisions.isEmpty()
                ? ResponseEntity.notFound().build()
                : ResponseEntity.ok(revisions);
    }

}
