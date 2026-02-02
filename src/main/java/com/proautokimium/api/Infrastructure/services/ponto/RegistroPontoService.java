package com.proautokimium.api.Infrastructure.services.ponto;

import com.proautokimium.api.Application.DTOs.ponto.RegistroPontoDTO;
import com.proautokimium.api.Application.DTOs.ponto.RegistroPontoRequestDTO;
import com.proautokimium.api.Application.DTOs.ponto.RegistroPontoUpdateDTO;
import com.proautokimium.api.Infrastructure.repositories.EmployeeRepository;
import com.proautokimium.api.Infrastructure.repositories.RegistroPontoRepository;
import com.proautokimium.api.domain.entities.Employee;
import com.proautokimium.api.domain.entities.RegistroPonto;
import jakarta.transaction.Transactional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class RegistroPontoService {

    @Autowired
    private RegistroPontoRepository registroPontoRepository;

    @Autowired
    private EmployeeRepository employeeRepository;

    public ResponseEntity<?> getRegistrosByEmployee(UUID employeeId, String mesAno){
        Employee employee = employeeRepository.findById(employeeId).orElse(null);

        if(employee == null){
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Funcionário não encontrado");
        }

        List<RegistroPonto> registros;

        if(mesAno != null && !mesAno.isEmpty()){
            registros = registroPontoRepository.findByEmployee_IdAndMesAnoOrderByDataDesc(employeeId, mesAno);
        }else {
            registros = registroPontoRepository.findByEmployee_IdOrderByDataDesc(employeeId);
        }

        List<RegistroPontoDTO> dtos = registros.stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());

        return ResponseEntity.ok(dtos);
    }

    @Transactional
    public ResponseEntity<?> createRegistro(UUID employeeId, RegistroPontoRequestDTO dto){
        Employee employee = employeeRepository.findById(employeeId).orElse(null);

        if(employee == null){
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Funcionário não encontrado");
        }

        var registroExistente = registroPontoRepository.findByEmployee_IdAndData(employeeId, dto.data());

        if(registroExistente.isPresent()){
            return ResponseEntity.status(HttpStatus.CONFLICT).body("já existe um registro para esta data");
        }

        RegistroPonto registro = new RegistroPonto();
        registro.setEmployee(employee);
        registro.setData(dto.data());
        registro.setEntrada(dto.entrada());
        registro.setAlmocoSaida(dto.almocoSaida());
        registro.setAlmocoRetorno(dto.almocoRetorno());
        registro.setSaida(dto.saida());

        RegistroPonto saved = registroPontoRepository.save(registro);

        return ResponseEntity.status(HttpStatus.CREATED).body(convertToDTO(saved));
    }

    @Transactional
    public ResponseEntity<?> updateRegistro(UUID registroId, UUID employeeId,
                                            RegistroPontoUpdateDTO dto) {
        RegistroPonto registro = registroPontoRepository.findById(registroId)
                .orElse(null);

        if (registro == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body("Registro não encontrado");
        }

        if (!registro.getEmployee().getId().equals(employeeId)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body("Registro não pertence a este funcionário");
        }

        if (dto.entrada() != null) {
            registro.setEntrada(dto.entrada());
        }
        if (dto.almocoSaida() != null) {
            registro.setAlmocoSaida(dto.almocoSaida());
        }
        if (dto.almocoRetorno() != null) {
            registro.setAlmocoRetorno(dto.almocoRetorno());
        }
        if (dto.saida() != null) {
            registro.setSaida(dto.saida());
        }

        RegistroPonto updated = registroPontoRepository.save(registro);

        return ResponseEntity.ok(convertToDTO(updated));
    }

    @Transactional
    public ResponseEntity<?> deleteRegistro(UUID registroId, UUID employeeId) {
        RegistroPonto registro = registroPontoRepository.findById(registroId)
                .orElse(null);

        if (registro == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body("Registro não encontrado");
        }

        if (!registro.getEmployee().getId().equals(employeeId)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body("Registro não pertence a este funcionário");
        }

        registroPontoRepository.delete(registro);

        return ResponseEntity.ok("Registro deletado com sucesso");
    }

    public ResponseEntity<?> getRegistroByData(UUID employeeId, LocalDate data) {
        var registro = registroPontoRepository
                .findByEmployee_IdAndData(employeeId, data);

        if (registro.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body("Registro não encontrado para esta data");
        }

        return ResponseEntity.ok(convertToDTO(registro.get()));
    }

    private RegistroPontoDTO convertToDTO(RegistroPonto registro) {
        return new RegistroPontoDTO(
                registro.getId(),
                registro.getData(),
                registro.getEntrada(),
                registro.getAlmocoSaida(),
                registro.getAlmocoRetorno(),
                registro.getSaida(),
                registro.calcularHorasTrabalhadas(),
                registro.getEmployee().getId()
        );
    }
}
