package com.proautokimium.api.controllers;

import com.proautokimium.api.Application.DTOs.partners.ServiceLocationDTO;
import com.proautokimium.api.Infrastructure.services.ServiceLocationService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

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
}
