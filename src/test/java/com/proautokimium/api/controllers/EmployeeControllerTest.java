package com.proautokimium.api.controllers;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.proautokimium.api.Application.DTOs.partners.EmployeeDTO;
import com.proautokimium.api.Infrastructure.repositories.UserRepository;
import com.proautokimium.api.Infrastructure.security.SecurityConfiguration;
import com.proautokimium.api.Infrastructure.security.TokenService;
import com.proautokimium.api.Infrastructure.services.partner.EmployeeService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

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

    private EmployeeDTO buildDto() {
        return new EmployeeDTO("EMP001", "12345678900", "Funcionario Teste", "func@teste.com", true, "MGR001", null, null, null);
    }

    @Test
    @DisplayName("GET /api/employee - deve retornar lista de funcionários quando autenticado")
    @WithMockUser
    void deveRetornarListaDeFuncionariosAutenticado() throws Exception {
        EmployeeDTO dto = buildDto();
        doReturn(ResponseEntity.ok(List.of(dto))).when(employeeService).getAllEmployes();

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
        doReturn(ResponseEntity.ok(List.of())).when(employeeService).getAllEmployesEmail();

        mockMvc.perform(get("/api/employee/only-email"))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("POST /api/employee - deve criar funcionário e retornar 200")
    @WithMockUser
    void deveCriarFuncionarioComSucesso() throws Exception {
        doReturn(ResponseEntity.ok("Funcionário criado com sucesso")).when(employeeService).createEmployee(any(EmployeeDTO.class));

        mockMvc.perform(post("/api/employee")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(buildDto())))
                .andExpect(status().isOk())
                .andExpect(content().string("Funcionário criado com sucesso"));
    }

    @Test
    @DisplayName("POST /api/employee - deve retornar 403 sem autenticação")
    void deveRetornar403AoCriarSemAutenticacao() throws Exception {
        mockMvc.perform(post("/api/employee")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(buildDto())))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("PUT /api/employee - deve atualizar funcionário e retornar 200")
    @WithMockUser
    void deveAtualizarFuncionarioComSucesso() throws Exception {
        doReturn(ResponseEntity.ok("Funcionário atualizado com sucesso")).when(employeeService).updateEmployee(any(EmployeeDTO.class));

        mockMvc.perform(put("/api/employee")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(buildDto())))
                .andExpect(status().isOk())
                .andExpect(content().string("Funcionário atualizado com sucesso"));
    }
}
