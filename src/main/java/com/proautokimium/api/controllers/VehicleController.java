package com.proautokimium.api.controllers;

import com.proautokimium.api.Application.DTOs.vehicle.VehicleRequestDTO;
import com.proautokimium.api.Infrastructure.repositories.VehicleRepository;
import com.proautokimium.api.domain.entities.Vehicle;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("api/vehicle")
public class VehicleController {

    @Autowired
    VehicleRepository repository;

    @PostMapping("create")
    public ResponseEntity CreateVehicle(@RequestBody @NotNull @Valid VehicleRequestDTO vehicleData){
        if(this.repository.findByPlaca(vehicleData.placa()) != null) return ResponseEntity.badRequest().build();

        Vehicle newVehicle = new Vehicle(
                vehicleData.nome(),
                vehicleData.placa(),
                vehicleData.marca(),
                vehicleData.consumoUrbanoAlcool(),
                vehicleData.consumoUrbanoGasolina(),
                vehicleData.consumoRodoviarioAlcool(),
                vehicleData.consumoRodoviarioGasolina()
        );
        this.repository.save(newVehicle);
        return ResponseEntity.ok().build();
    }

    @GetMapping("get-all")
    public ResponseEntity getAllVehicle(){
        var vehicleList = this.repository.findAll();
        return ResponseEntity.ok(vehicleList);
    }
}
