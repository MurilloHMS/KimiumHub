package com.proautokimium.api.controllers;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.proautokimium.api.Infrastructure.services.permission.PermissionService;
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
    @MockitoBean com.proautokimium.api.Infrastructure.services.partner.ErpPartnerLookupService erpPartnerLookup;
    @MockitoBean TokenService tokenService;
    // O SecurityFilter passa a somar as permissões de tela às roles.
    @MockitoBean PermissionService permissionService;
    @MockitoBean AuthenticationManager authenticationManager;
    @MockitoBean UserRepository userRepository;

    private EmployeeDTO buildUpdateDto() {
        return new EmployeeDTO("EMP001", "12345678900", "Funcionario Teste", "func@teste.com", true, "MGR001", null, null, null, null, null,
                null, null, null, null, null, null);
    }

    private CreateEmployeeRequestDTO buildCreateDto() {
        return new CreateEmployeeRequestDTO(
                "EMP001", "12345678900", "Funcionario Teste", "func@teste.com", true, "MGR001",
                null, LocalDate.of(1990, 1, 1), null,
                UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(),
                ContractType.CLT, LocalDate.now(),
                null, null, null, null, null, null
        );
    }

    private EmployeeResponseDTO buildResponseDto() {
        return new EmployeeResponseDTO(UUID.randomUUID(), "EMP001", "12345678900", "Funcionario Teste",
                "func@teste.com", true, "MGR001", null, null, null, null, null,
                null, null, null, null, null, null, null,
                null, null, null, null, null, null, null);
    }

    @Test
    @DisplayName("GET /api/employee - deve retornar lista de funcionários quando autenticado")
    @WithMockUser(authorities = {"rh/employees:CONSULTAR", "rh/employees:INCLUIR", "rh/employees:ALTERAR"})
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
    @WithMockUser(authorities = {"rh/employees:CONSULTAR", "rh/employees:INCLUIR", "rh/employees:ALTERAR"})
    void deveRetornarEmailsDeFuncionarios() throws Exception {
        doReturn(List.of()).when(employeeService).getAllEmployesEmail();

        mockMvc.perform(get("/api/employee/only-email"))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("POST /api/employee - deve criar funcionário e retornar 201")
    @WithMockUser(authorities = {"rh/employees:CONSULTAR", "rh/employees:INCLUIR", "rh/employees:ALTERAR"})
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
    @WithMockUser(authorities = {"rh/employees:CONSULTAR", "rh/employees:INCLUIR", "rh/employees:ALTERAR"})
    void deveAtualizarFuncionarioComSucesso() throws Exception {
        doReturn(buildResponseDto()).when(employeeService).updateEmployee(any(EmployeeDTO.class));

        mockMvc.perform(put("/api/employee")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(buildUpdateDto())))
                .andExpect(status().isOk());
    }

    // ── Buscar parceiro no Sankhya ───────────────────────────────────────────

    /**
     * <b>INCLUIR, e não CONSULTAR.</b> Com CONSULTAR, qualquer um que vê a lista
     * de funcionários poderia varrer o ERP um código por vez colhendo nome, CPF
     * e e-mail. A rota existe dentro do formulário de cadastro, e a permissão
     * acompanha o uso.
     */
    @Test
    @DisplayName("GET /api/employee/erp/{cod} - 403 para quem só consulta")
    @WithMockUser(authorities = {"rh/employees:CONSULTAR"})
    void erpLookupDeniedForReadOnly() throws Exception {
        mockMvc.perform(get("/api/employee/erp/3418"))
                .andExpect(status().isForbidden());

        verify(erpPartnerLookup, never()).byCode(anyInt());
    }

    @Test
    @DisplayName("GET /api/employee/erp/{cod} - devolve o parceiro com INCLUIR")
    @WithMockUser(authorities = {"rh/employees:INCLUIR"})
    void erpLookupReturnsPartner() throws Exception {
        when(erpPartnerLookup.byCode(3418)).thenReturn(
                new com.proautokimium.api.Application.DTOs.partners.ErpPartnerDTO(
                        "3418", "JOSE CARLOS", "82111440830", "jose@x.com", true, null, null));

        mockMvc.perform(get("/api/employee/erp/3418"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("JOSE CARLOS"))
                .andExpect(jsonPath("$.document").value("82111440830"));
    }

    /**
     * <b>A guarda contra injeção.</b> O código vem da URL e vira parte de um
     * DECLARE no SQL. Com o parâmetro tipado como int, o Spring recusa o que não
     * for numérico ANTES de o método rodar — e antes de qualquer coisa chegar ao
     * Sankhya.
     *
     * <p>O {@code verify} é a metade que importa: sem ele, o teste passaria
     * mesmo que o texto fosse concatenado e a consulta saísse.
     */
    @Test
    @DisplayName("GET /api/employee/erp/{cod} - código não numérico não chega ao ERP")
    @WithMockUser(authorities = {"rh/employees:INCLUIR"})
    void erpLookupRejectsNonNumeric() throws Exception {
        // "abc" e não uma URL com aspas e ponto-e-vírgula: aquela é rejeitada
        // antes de chegar ao handler, e o teste passaria com qualquer
        // implementação. Esta chega, e separa `int` de String com parseInt —
        // que daria 500.
        mockMvc.perform(get("/api/employee/erp/abc"))
                .andExpect(status().isBadRequest());

        verify(erpPartnerLookup, never()).byCode(anyInt());
    }
}
