package com.proautokimium.api.controllers.prostock;

import com.proautokimium.api.Application.DTOs.prostock.machine.CreateRegisterDTO;
import com.proautokimium.api.Application.DTOs.prostock.machine.MachineDTO;
import com.proautokimium.api.Application.DTOs.prostock.machine.MachineMovementDTO;
import com.proautokimium.api.Application.DTOs.prostock.machine.UpdateRegisterDTO;
import com.proautokimium.api.Infrastructure.services.machine.MachineService;
import com.proautokimium.api.Infrastructure.services.machine.RegisterService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("api/machine")
public class MachineController {

    @Autowired
    private MachineService service;

    @Autowired
    private RegisterService registerService;

    @GetMapping
    public ResponseEntity<Object> getMachines(){
        return ResponseEntity.status(HttpStatus.OK).body(service.getAllMachines());
    }

    @PostMapping
    public ResponseEntity<Object> createMachine(@RequestBody @Valid @NotNull MachineDTO dto){
        service.save(dto);
        return ResponseEntity.status(HttpStatus.CREATED).body("Máquina Cadastrada com sucesso!");
    }

    @PutMapping
    public ResponseEntity<Object> updateMachine(@RequestBody @Valid @NotNull MachineDTO dto){
        service.update(dto);
        return ResponseEntity.status(HttpStatus.OK).body("Máquina Atualizada com sucesso!");
    }

    @DeleteMapping("{id}")
    public ResponseEntity<Object> deleteMachine(@PathVariable UUID id){
        service.delete(id);
        return ResponseEntity.status(HttpStatus.OK).body("Máquina Deletada com sucesso!");
    }

    // Movements

    @GetMapping("movements/{id}")
    public ResponseEntity<Object> getMovementsByMachine(@PathVariable @NotNull UUID id){
        return ResponseEntity.status(HttpStatus.OK).body(service.getMovementsByMachineId(id));
    }

    @PostMapping("movements/{id}")
    public ResponseEntity<Object> createMovement(@RequestBody MachineMovementDTO dto, @PathVariable @NotNull UUID id){
        service.createMovement(dto, id);
        return ResponseEntity.status(HttpStatus.CREATED).body("Movimentação da máquina incluída com sucesso");
    }

    @PutMapping("movements/{id}")
    public ResponseEntity<Object> updateMovement(@RequestBody MachineMovementDTO dto, @PathVariable @NotNull UUID id){
        service.updateMovement(dto, id);
        return ResponseEntity.status(HttpStatus.OK).body("Movimentação da máquina atualizada com sucesso");
    }

    @DeleteMapping("movements/{id}")
    public ResponseEntity<Object> deleteMovement(@PathVariable @NotNull UUID id){
        service.deleteMovement(id);
        return ResponseEntity.status(HttpStatus.OK).body("Movimento da máquina Deletado com sucesso!");
    }

    // Registers

    @PostMapping("/register")
    public ResponseEntity<?> createRegister(@RequestBody CreateRegisterDTO dto){
        registerService.create(dto);
        return ResponseEntity.status(HttpStatus.CREATED).body("Registro da máquina incluído com sucesso");
    }

    @PutMapping("/register/{id}")
    public ResponseEntity<?> updateRegister(@RequestBody UpdateRegisterDTO dto, @PathVariable UUID id){
        registerService.update(dto, id);
        return ResponseEntity.status(HttpStatus.OK).body("Registro da máquina atualizado com sucesso");
    }

    @DeleteMapping("/register/{id}")
    public ResponseEntity<?> deleteRegister(@PathVariable UUID id){
        registerService.delete(id);
        return ResponseEntity.status(HttpStatus.OK).body("Registro da máquina deletado com sucesso");
    }

    @GetMapping("/register/{id}")
    public ResponseEntity<?> getRegistersByMachineId(@PathVariable UUID id){
        return ResponseEntity.ok(registerService.listarRegistrosPorMaquina(id));
    }
}
