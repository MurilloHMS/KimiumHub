package com.proautokimium.api.controllers.prostock;

import com.proautokimium.api.Application.DTOs.machine.MachineDTO;
import com.proautokimium.api.Infrastructure.services.machine.MachineService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("api/machine")
public class MachineController {

    @Autowired
    private MachineService service;

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

}
