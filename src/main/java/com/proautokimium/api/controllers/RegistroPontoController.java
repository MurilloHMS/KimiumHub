package com.proautokimium.api.controllers;

import com.proautokimium.api.Application.DTOs.ponto.RegistroPontoRequestDTO;
import com.proautokimium.api.Application.DTOs.ponto.RegistroPontoUpdateDTO;
import com.proautokimium.api.Infrastructure.repositories.EmployeeRepository;
import com.proautokimium.api.Infrastructure.services.ponto.RegistroPontoService;
import com.proautokimium.api.domain.entities.Employee;
import com.proautokimium.api.domain.entities.User;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.UUID;

@RestController
@RequestMapping("api/ponto")
public class RegistroPontoController {

    @Autowired
    private RegistroPontoService registroPontoService;

    @Autowired
    private EmployeeRepository employeeRepository;

    @GetMapping("me")
    public ResponseEntity<?> getMyRegistros(
            @AuthenticationPrincipal User user,
            @RequestParam(required = false) String mesAno) {

        Employee employee = employeeRepository.findByUsername(user.getLogin()).orElse(null);

        if (employee == null){
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Funcionário não encontrado");
        }

        return registroPontoService.getRegistrosByEmployee(employee.getId(), mesAno);
    }

    @GetMapping("/me/data/{data}")
    public ResponseEntity<?> getMyRegistroByData(
            @AuthenticationPrincipal User user,
            @PathVariable @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate data) {

        Employee employee = employeeRepository.findByUsername(user.getLogin()).orElse(null);

        if (employee == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body("Funcionário não encontrado");
        }

        return registroPontoService.getRegistroByData(employee.getId(), data);
    }

    @PostMapping("/me")
    public ResponseEntity<?> createMyRegistro(
            @AuthenticationPrincipal User user,
            @RequestBody @Valid RegistroPontoRequestDTO dto) {

        Employee employee = employeeRepository.findByUsername(user.getLogin()).orElse(null);

        if (employee == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body("Funcionário não encontrado");
        }

        return registroPontoService.createRegistro(employee.getId(), dto);
    }

    @PatchMapping("/me/{id}")
    public ResponseEntity<?> updateMyRegistro(
            @AuthenticationPrincipal User user,
            @PathVariable UUID id,
            @RequestBody @Valid RegistroPontoUpdateDTO dto) {

        Employee employee = employeeRepository.findByUsername(user.getLogin()).orElse(null);

        if (employee == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body("Funcionário não encontrado");
        }

        return registroPontoService.updateRegistro(id, employee.getId(), dto);
    }

    @DeleteMapping("/me/{id}")
    public ResponseEntity<?> deleteMyRegistro(
            @AuthenticationPrincipal User user,
            @PathVariable UUID id) {

        Employee employee = employeeRepository.findByUsername(user.getLogin()).orElse(null);

        if (employee == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body("Funcionário não encontrado");
        }

        return registroPontoService.deleteRegistro(id, employee.getId());
    }

}
