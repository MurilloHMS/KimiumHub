package com.proautokimium.api.controllers.processoSeletivo;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.proautokimium.api.Application.DTOs.processoSeletivo.candidato.CreateCandidatoDTO;
import com.proautokimium.api.Application.DTOs.processoSeletivo.candidato.ResponseCandidatoDTO;
import com.proautokimium.api.Infrastructure.exceptions.processoSeletivo.CandidatoAlreadyExistsException;
import com.proautokimium.api.domain.entities.processoSeletivo.Candidato;
import com.proautokimium.api.Infrastructure.repositories.UserRepository;
import com.proautokimium.api.Infrastructure.security.SecurityConfiguration;
import com.proautokimium.api.Infrastructure.security.TokenService;
import com.proautokimium.api.Infrastructure.services.processoSeletivo.CandidatoService;
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

import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(CandidatoController.class)
@TestPropertySource(properties = {"server.port=0"})
@Import(SecurityConfiguration.class)
class CandidatoControllerTest {

    @Autowired MockMvc mockMvc;
    @Autowired ObjectMapper objectMapper;
    @MockitoBean CandidatoService candidatoService;
    @MockitoBean TokenService tokenService;
    @MockitoBean AuthenticationManager authenticationManager;
    @MockitoBean UserRepository userRepository;

    @Test
    @DisplayName("POST /api/candidato - deve cadastrar candidato e retornar 200")
    @WithMockUser
    void deveCadastrarCandidatoComSucesso() throws Exception {
        CreateCandidatoDTO dto = new CreateCandidatoDTO("João Silva", "joao@teste.com", "11999999999", "linkedin.com/in/joao", "curriculo.pdf");
        when(candidatoService.create(any(CreateCandidatoDTO.class))).thenReturn(mock(Candidato.class));

        mockMvc.perform(post("/api/candidato")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isOk())
                .andExpect(content().string("Candidato cadastrado com sucesso"));
    }

    @Test
    @DisplayName("POST /api/candidato - deve retornar 403 sem autenticação")
    void deveRetornar403SemAutenticacao() throws Exception {
        CreateCandidatoDTO dto = new CreateCandidatoDTO("João Silva", "joao@teste.com", "11999999999", null, null);

        mockMvc.perform(post("/api/candidato")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("POST /api/candidato - deve retornar 409 quando candidato já existe")
    @WithMockUser
    void deveRetornarErroAoCadastrarCandidatoDuplicado() throws Exception {
        CreateCandidatoDTO dto = new CreateCandidatoDTO("João Silva", "joao@teste.com", "11999999999", null, null);
        doThrow(new CandidatoAlreadyExistsException()).when(candidatoService).create(any(CreateCandidatoDTO.class));

        mockMvc.perform(post("/api/candidato")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isConflict());
    }

    @Test
    @DisplayName("GET /api/candidato - deve retornar lista de candidatos quando autenticado")
    @WithMockUser
    void deveRetornarListaDeCandidatos() throws Exception {
        ResponseCandidatoDTO candidato = new ResponseCandidatoDTO("João Silva", "joao@teste.com", "11999999999", null, null, LocalDateTime.now());
        when(candidatoService.listarCandidatos()).thenReturn(List.of(candidato));

        mockMvc.perform(get("/api/candidato"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].nome").value("João Silva"))
                .andExpect(jsonPath("$[0].email").value("joao@teste.com"));
    }

    @Test
    @DisplayName("GET /api/candidato - deve retornar 403 sem autenticação")
    void deveRetornar403AoListarSemAutenticacao() throws Exception {
        mockMvc.perform(get("/api/candidato"))
                .andExpect(status().isForbidden());
    }
}
