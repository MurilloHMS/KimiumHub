package com.proautokimium.api.controllers;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.proautokimium.api.Application.DTOs.partners.CustomerRequestDTO;
import com.proautokimium.api.Application.DTOs.partners.PartnerRecipientDTO;
import com.proautokimium.api.Infrastructure.repositories.UserRepository;
import com.proautokimium.api.Infrastructure.security.SecurityConfiguration;
import com.proautokimium.api.Infrastructure.security.TokenService;
import com.proautokimium.api.Infrastructure.services.partner.CustomerService;
import com.proautokimium.api.domain.exceptions.customer.CustomerAlreadyExistsException;
import com.proautokimium.api.domain.exceptions.customer.CustomerNotFoundException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(CustomerController.class)
@TestPropertySource(properties = {"server.port=0"})
@Import(SecurityConfiguration.class)
class CustomerControllerTest {

    @Autowired MockMvc mockMvc;
    @Autowired ObjectMapper objectMapper;
    @MockitoBean CustomerService customerService;
    @MockitoBean TokenService tokenService;
    @MockitoBean AuthenticationManager authenticationManager;
    @MockitoBean UserRepository userRepository;

    private CustomerRequestDTO buildDto() {
        return new CustomerRequestDTO("COD001", "12345678000100", "Cliente Teste", "cliente@teste.com", "user", true, true, "MAT001");
    }

    @Test
    @DisplayName("POST /api/customer - deve cadastrar cliente e retornar 201")
    @WithMockUser
    void deveCadastrarClienteComSucesso() throws Exception {
        doNothing().when(customerService).createCustomer(any(CustomerRequestDTO.class));

        mockMvc.perform(post("/api/customer")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(buildDto())))
                .andExpect(status().isCreated())
                .andExpect(content().string("Cliente cadastrado com sucesso!"));
    }

    @Test
    @DisplayName("POST /api/customer - deve retornar 403 sem autenticação")
    void deveRetornar403AoCadastrarSemAutenticacao() throws Exception {
        mockMvc.perform(post("/api/customer")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(buildDto())))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("POST /api/customer - deve retornar 409 quando cliente já existe")
    @WithMockUser
    void deveRetornarErroAoCadastrarClienteDuplicado() throws Exception {
        doThrow(new CustomerAlreadyExistsException()).when(customerService).createCustomer(any(CustomerRequestDTO.class));

        mockMvc.perform(post("/api/customer")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(buildDto())))
                .andExpect(status().isConflict());
    }

    @Test
    @DisplayName("POST /api/customer/upload - deve importar clientes via Excel e retornar 201")
    @WithMockUser
    void deveImportarClientesViaExcel() throws Exception {
        MockMultipartFile file = new MockMultipartFile("file", "customers.xlsx",
                "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet", new byte[]{1, 2, 3});
        doNothing().when(customerService).includeCustomersByExcel(any());

        mockMvc.perform(multipart("/api/customer/upload")
                        .file(file)
                        .with(csrf()))
                .andExpect(status().isCreated())
                .andExpect(content().string("Clientes cadastrado com sucesso via planilha!"));
    }

    @Test
    @DisplayName("GET /api/customer - deve retornar lista de clientes quando autenticado")
    @WithMockUser
    void deveRetornarListaDeClientesAutenticado() throws Exception {
        when(customerService.getAllCustomers()).thenReturn(ResponseEntity.ok(List.of(buildDto())));

        mockMvc.perform(get("/api/customer"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].codParceiro").value("COD001"))
                .andExpect(jsonPath("$[0].nome").value("Cliente Teste"));
    }

    @Test
    @DisplayName("GET /api/customer - deve retornar 403 sem autenticação")
    void deveRetornar403AoListarSemAutenticacao() throws Exception {
        mockMvc.perform(get("/api/customer"))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("GET /api/customer/only-email - deve retornar lista de emails dos clientes")
    @WithMockUser
    void deveRetornarListaDeEmailsDeClientes() throws Exception {
        PartnerRecipientDTO emailDto = new PartnerRecipientDTO(UUID.randomUUID(), "Cliente Teste", "cliente@teste.com", "customer");
        when(customerService.getAllCustomersEmail()).thenReturn(ResponseEntity.ok(List.of(emailDto)));

        mockMvc.perform(get("/api/customer/only-email"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].email").value("cliente@teste.com"))
                .andExpect(jsonPath("$[0].type").value("customer"));
    }

    @Test
    @DisplayName("PUT /api/customer - deve atualizar cliente e retornar 200")
    @WithMockUser
    void deveAtualizarClienteComSucesso() throws Exception {
        doNothing().when(customerService).UpdateCustomer(any(CustomerRequestDTO.class));

        mockMvc.perform(put("/api/customer")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(buildDto())))
                .andExpect(status().isOk())
                .andExpect(content().string("Cliente atualizado com sucesso!"));
    }

    @Test
    @DisplayName("PUT /api/customer - deve retornar 404 quando cliente não existe")
    @WithMockUser
    void deveRetornarErroAoAtualizarClienteInexistente() throws Exception {
        doThrow(new CustomerNotFoundException()).when(customerService).UpdateCustomer(any(CustomerRequestDTO.class));

        mockMvc.perform(put("/api/customer")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(buildDto())))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("DELETE /api/customer - deve deletar cliente e retornar 200")
    @WithMockUser
    void deveDeletarClienteComSucesso() throws Exception {
        doNothing().when(customerService).DeleteCustomer(any(String.class));

        mockMvc.perform(delete("/api/customer")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("\"COD001\""))
                .andExpect(status().isOk())
                .andExpect(content().string("Cliente deletado com sucesso!"));
    }

    @Test
    @DisplayName("DELETE /api/customer - deve retornar 404 quando cliente não existe")
    @WithMockUser
    void deveRetornarErroAoDeletarClienteInexistente() throws Exception {
        doThrow(new CustomerNotFoundException()).when(customerService).DeleteCustomer(any(String.class));

        mockMvc.perform(delete("/api/customer")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("\"COD001\""))
                .andExpect(status().isNotFound());
    }
}
