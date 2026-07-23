package com.proautokimium.api.controllers;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.proautokimium.api.Application.DTOs.partners.CreateEmployeeRequestDTO;
import com.proautokimium.api.Application.DTOs.partners.EmployeeDTO;
import com.proautokimium.api.Application.DTOs.partners.EmployeeResponseDTO;
import com.proautokimium.api.Infrastructure.repositories.UserRepository;
import com.proautokimium.api.Infrastructure.security.SecurityConfiguration;
import com.proautokimium.api.Infrastructure.security.TokenService;
import com.proautokimium.api.Infrastructure.services.partner.EmployeeService;
import com.proautokimium.api.domain.enums.humanResources.ContractType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(EmployeeController.class)
@TestPropertySource(properties = {"server.port=0"})
@Import(SecurityConfiguration.class)
class EmployeeControllerTest {

    @Autowired MockMvc mockMvc;
    @Autowired ObjectMapper objectMapper;
    @MockitoBean EmployeeService employeeService;
    @MockitoBean TokenService tokenService;
    @MockitoBean AuthenticationManager authenticationManager;
    @MockitoBean UserRepository userRepository;

    private EmployeeDTO buildUpdateDto() {
        return new EmployeeDTO("EMP001", "12345678900", "Funcionario Teste", "func@teste.com", true, "MGR001", null, null, null, null, null);
    }

    private CreateEmployeeRequestDTO buildCreateDto() {
        return new CreateEmployeeRequestDTO(
                "EMP001", "12345678900", "Funcionario Teste", "func@teste.com", true, "MGR001",
                null, LocalDate.of(1990, 1, 1), null,
                UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(),
                ContractType.CLT, LocalDate.now()
        );
    }

    private EmployeeResponseDTO buildResponseDto() {
        return new EmployeeResponseDTO(UUID.randomUUID(), "EMP001", "12345678900", "Funcionario Teste",
                "func@teste.com", true, "MGR001", null, null, null, null, null);
    }

    @Test
    @DisplayName("GET /api/employee - deve retornar lista de funcionários quando autenticado")
    @WithMockUser
    void deveRetornarListaDeFuncionariosAutenticado() throws Exception {
        doReturn(List.of(buildResponseDto())).when(employeeService).getAllEmployes();

        mockMvc.perform(get("/api/employee"))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("GET /api/employee - deve retornar 403 sem autenticação")
    void deveRetornar403SemAutenticacao() throws Exception {
        mockMvc.perform(get("/api/employee"))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("GET /api/employee/only-email - deve retornar emails dos funcionários")
    @WithMockUser
    void deveRetornarEmailsDeFuncionarios() throws Exception {
        doReturn(List.of()).when(employeeService).getAllEmployesEmail();

        mockMvc.perform(get("/api/employee/only-email"))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("POST /api/employee - deve criar funcionário e retornar 201")
    @WithMockUser
    void deveCriarFuncionarioComSucesso() throws Exception {
        doReturn(buildResponseDto()).when(employeeService).createEmployee(any(CreateEmployeeRequestDTO.class));

        mockMvc.perform(post("/api/employee")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(buildCreateDto())))
                .andExpect(status().isCreated());
    }

    @Test
    @DisplayName("POST /api/employee - deve retornar 403 sem autenticação")
    void deveRetornar403AoCriarSemAutenticacao() throws Exception {
        mockMvc.perform(post("/api/employee")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(buildCreateDto())))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("PUT /api/employee - deve atualizar funcionário e retornar 200")
    @WithMockUser
    void deveAtualizarFuncionarioComSucesso() throws Exception {
        doReturn(buildResponseDto()).when(employeeService).updateEmployee(any(EmployeeDTO.class));

        mockMvc.perform(put("/api/employee")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(buildUpdateDto())))
                .andExpect(status().isOk());
    }
}
