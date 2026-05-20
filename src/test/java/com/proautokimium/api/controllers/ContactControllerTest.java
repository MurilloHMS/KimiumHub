package com.proautokimium.api.controllers;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.proautokimium.api.Application.DTOs.contact.CreateContactDTO;
import com.proautokimium.api.Infrastructure.repositories.UserRepository;
import com.proautokimium.api.Infrastructure.security.SecurityConfiguration;
import com.proautokimium.api.Infrastructure.security.TokenService;
import com.proautokimium.api.Infrastructure.services.ContactService;
import com.proautokimium.api.domain.enums.ContactStatus;
import com.proautokimium.api.domain.enums.ContactType;
import com.proautokimium.api.domain.valueObjects.Email;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(ContactController.class)
@Import(SecurityConfiguration.class)
@ActiveProfiles("test")
class ContactControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private TokenService tokenService;

    @MockitoBean
    private UserRepository userRepository;

    @MockitoBean
    private ContactService service;

    @Autowired
    private ObjectMapper objectMapper;


    @Test
    @DisplayName("Deve registrar um novo contato com sucesso")
    void create() throws Exception {
        CreateContactDTO dto = new CreateContactDTO(
                "teste",
                "teste@teste.com",
                ContactType.DuvidaProduto,
                null,
                "mensagem de teste",
                "empresa de teste",
                ContactStatus.AguardandoRetorno,
                LocalDateTime.now());

        mockMvc.perform(post("/api/contact")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isCreated());
    }

    @Test
    @DisplayName("Deve retornar erro se o email for inválido")
    void shouldReturnErrorWithEmailInvalid() throws Exception {

        doThrow(new IllegalArgumentException("Invalid email address"))
                .when(service)
                .createContact(any());

        CreateContactDTO dto = new CreateContactDTO(
                "teste",
                "teste@teste",
                ContactType.DuvidaProduto,
                null,
                "mensagem de teste",
                "empresa de teste",
                ContactStatus.AguardandoRetorno,
                LocalDateTime.now());

        mockMvc.perform(post("/api/contact")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isInternalServerError());
    }
}