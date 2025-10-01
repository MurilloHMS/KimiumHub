package com.proautokimium.api.controllers;

import com.proautokimium.api.Application.DTOs.partners.ServiceLocationDTO;
import com.proautokimium.api.Infrastructure.services.ServiceLocationService;
import com.proautokimium.api.domain.entities.ServiceLocation;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import org.apache.coyote.Response;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("api/service-locations")
public class ServiceLocationController {

    @Autowired
    private ServiceLocationService serviceLocationService;

    @PostMapping
    public ResponseEntity<Object> createServiceLocation(@RequestBody @NotNull @Valid ServiceLocationDTO serviceLocationDTO) {
        serviceLocationService.createServiceLocation(serviceLocationDTO);
        return ResponseEntity.status(201).build();
    }

    @DeleteMapping("{name}")
    public ResponseEntity<Object> deleteServiceLocation(@RequestParam @NotNull String systemCode) {
        serviceLocationService.deleteServiceLocationBySystemCode(systemCode);
        return ResponseEntity.status(204).build();
    }

    @GetMapping
    public ResponseEntity<Object> getAllServiceLocations(){
        var serviceLocations = serviceLocationService.getAllServiceLocations();

        var sl = serviceLocations.stream().map(m -> new ServiceLocationDTO(
           m.getCodParceiro(),
           m.getDocumento(),
           m.getName(),
           m.getEmail().getAddress(),
           m.isAtivo(),
           m.getAddress()
        )).toList();

        return ResponseEntity.ok(sl);
    }

    @PutMapping
    public ResponseEntity<Object> updateServiceLocations(@RequestBody @NotNull @Valid ServiceLocationDTO serviceLocationDTO){
        serviceLocationService.updateServiceLocation(serviceLocationDTO);
        return ResponseEntity.ok().build();
    }
}
