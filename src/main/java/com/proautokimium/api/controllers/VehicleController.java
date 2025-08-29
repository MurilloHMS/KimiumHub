package com.proautokimium.api.controllers;

import com.proautokimium.api.Application.DTOs.vehicle.VehicleRequestDTO;
import com.proautokimium.api.Infrastructure.repositories.VehicleRepository;
import com.proautokimium.api.domain.entities.Vehicle;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
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

    @PostMapping
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

    @GetMapping
    public ResponseEntity getAllVehicle(){
        var vehicleList = this.repository.findAll();
        return ResponseEntity.ok(vehicleList);
    }

    @PutMapping
    public ResponseEntity updateVehicle(@RequestBody @NotNull @Valid VehicleRequestDTO vehicleData){
        var vehicle = this.repository.findByPlaca(vehicleData.placa());
        if(vehicle == null) return ResponseEntity.badRequest().build();

        vehicle.setNome(vehicleData.nome());
        vehicle.setPlaca(vehicleData.placa());
        vehicle.setMarca(vehicleData.marca());
        vehicle.setConsumoRodoviarioAlcool(vehicleData.consumoRodoviarioAlcool());
        vehicle.setConsumoRodoviarioGasolina(vehicleData.consumoRodoviarioGasolina());
        vehicle.setConsumoUrbanoAlcool(vehicleData.consumoUrbanoAlcool());
        vehicle.setConsumoUrbanoGasolina(vehicleData.consumoUrbanoGasolina());

        this.repository.save(vehicle);
        return ResponseEntity.ok("Vehicle has successfully update");
    }
    
    @DeleteMapping
    public ResponseEntity deleteVehicle(@RequestBody @NotNull @Valid VehicleRequestDTO vehicleData){
        var vehicle = this.repository.findByPlaca(vehicleData.placa());
        if(vehicle == null) return ResponseEntity.notFound().build();

        this.repository.deleteById(vehicle.getId());
        return ResponseEntity.ok("vehicle has successfully deleted");
    }
}
