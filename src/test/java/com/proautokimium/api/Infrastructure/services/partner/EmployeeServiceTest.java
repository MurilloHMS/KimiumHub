package com.proautokimium.api.Infrastructure.services.partner;

import com.proautokimium.api.Application.DTOs.partners.EmployeeDTO;
import com.proautokimium.api.Infrastructure.repositories.EmployeeRepository;
import com.proautokimium.api.domain.entities.Employee;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.List;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class EmployeeServiceTest {

    @Mock
    private EmployeeRepository repository;

    @InjectMocks
    private EmployeeService employeeService;

    private EmployeeDTO dto;
    private Employee employee;

    @BeforeEach
    void setUp() {
        dto = new EmployeeDTO("EMP001", "12345678900", "Funcionario Teste", "func@teste.com", true, "MGR001", null, null, null);
        employee = mock(Employee.class);
    }

    @Test
    @DisplayName("Deve criar funcionário com sucesso e retornar 200")
    void deveCriarFuncionarioComSucesso() {
        when(repository.save(any(Employee.class))).thenReturn(employee);

        ResponseEntity<?> response = employeeService.createEmployee(dto);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isEqualTo("Funcionário criado com sucesso");
        verify(repository).save(any(Employee.class));
    }

    @Test
    @DisplayName("Deve retornar 400 ao criar funcionário com email inválido")
    void deveRetornarBadRequestComEmailInvalido() {
        EmployeeDTO dtoInvalido = new EmployeeDTO("EMP001", "12345678900", "Teste", "email-invalido", true, null, null, null, null);

        ResponseEntity<?> response = employeeService.createEmployee(dtoInvalido);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        verify(repository, never()).save(any());
    }

    @Test
    @DisplayName("Deve atualizar funcionário com sucesso e retornar 200")
    void deveAtualizarFuncionarioComSucesso() {
        when(repository.findByCodParceiro("EMP001")).thenReturn(employee);
        when(repository.save(employee)).thenReturn(employee);

        ResponseEntity<?> response = employeeService.updateEmployee(dto);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isEqualTo("Funcionário atualizado com sucesso");
        verify(repository).save(employee);
    }

    @Test
    @DisplayName("Deve retornar 400 ao atualizar funcionário inexistente")
    void deveRetornarBadRequestAoAtualizarFuncionarioInexistente() {
        when(repository.findByCodParceiro("EMP001")).thenReturn(null);

        ResponseEntity<?> response = employeeService.updateEmployee(dto);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    }

    @Test
    @DisplayName("Deve retornar lista de funcionários com sucesso")
    void deveRetornarListaDeFuncionarios() {
        when(employee.getCodParceiro()).thenReturn("EMP001");
        when(employee.getDocumento()).thenReturn("12345678900");
        when(employee.getName()).thenReturn("Funcionario Teste");
        when(employee.getEmail()).thenReturn(mock(com.proautokimium.api.domain.valueObjects.Email.class));
        when(employee.getEmail().getAddress()).thenReturn("func@teste.com");
        when(employee.isAtivo()).thenReturn(true);
        when(employee.getCodigoGerente()).thenReturn("MGR001");
        when(employee.getHierarquia()).thenReturn(null);
        when(employee.getBirthday()).thenReturn(null);
        when(employee.getDepartment()).thenReturn(null);
        when(repository.findAll()).thenReturn(List.of(employee));

        ResponseEntity<?> response = employeeService.getAllEmployes();

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    }

    @Test
    @DisplayName("Deve retornar lista de emails de funcionários com sucesso")
    void deveRetornarListaDeEmailsDeFuncionarios() {
        com.proautokimium.api.domain.valueObjects.Email emailVO = mock(com.proautokimium.api.domain.valueObjects.Email.class);
        when(employee.getId()).thenReturn(java.util.UUID.randomUUID());
        when(employee.getName()).thenReturn("Funcionario Teste");
        when(employee.getEmail()).thenReturn(emailVO);
        when(emailVO.getAddress()).thenReturn("func@teste.com");
        when(repository.findAll()).thenReturn(List.of(employee));

        ResponseEntity<?> response = employeeService.getAllEmployesEmail();

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    }
}
