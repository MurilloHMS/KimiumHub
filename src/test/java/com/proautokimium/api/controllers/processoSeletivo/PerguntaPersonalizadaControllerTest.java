package com.proautokimium.api.controllers.processoSeletivo;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.proautokimium.api.Application.DTOs.processoSeletivo.perguntas.CreatePerguntaDTO;
import com.proautokimium.api.Application.DTOs.processoSeletivo.perguntas.UpdatePerguntaDTO;
import com.proautokimium.api.Infrastructure.repositories.UserRepository;
import com.proautokimium.api.Infrastructure.security.SecurityConfiguration;
import com.proautokimium.api.Infrastructure.security.TokenService;
import com.proautokimium.api.Infrastructure.services.processoSeletivo.PerguntaPersonalizadaService;
import com.proautokimium.api.domain.enums.processoSeletivo.TipoPergunta;
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

import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(PerguntaPersonalizadaController.class)
@TestPropertySource(properties = {"server.port=0"})
@Import(SecurityConfiguration.class)
class PerguntaPersonalizadaControllerTest {

    @Autowired MockMvc mockMvc;
    @Autowired ObjectMapper objectMapper;
    @MockitoBean PerguntaPersonalizadaService perguntaService;
    @MockitoBean TokenService tokenService;
    @MockitoBean AuthenticationManager authenticationManager;
    @MockitoBean UserRepository userRepository;

    @Test
    @DisplayName("POST /api/pergunta/{id} - deve criar pergunta e retornar 200")
    @WithMockUser
    void deveCriarPerguntaComSucesso() throws Exception {
        UUID vagaId = UUID.randomUUID();
        CreatePerguntaDTO dto = new CreatePerguntaDTO("Qual sua experiência?", TipoPergunta.TEXTO_LIVRE, true, (short) 1);
        doNothing().when(perguntaService).create(any(CreatePerguntaDTO.class), any(UUID.class));

        mockMvc.perform(post("/api/pergunta/{id}", vagaId)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isOk())
                .andExpect(content().string("Pergunta criada com sucesso"));
    }

    @Test
    @DisplayName("POST /api/pergunta/{id} - deve retornar 403 sem autenticação")
    void deveRetornar403AoCriarSemAutenticacao() throws Exception {
        UUID vagaId = UUID.randomUUID();
        CreatePerguntaDTO dto = new CreatePerguntaDTO("Qual sua experiência?", TipoPergunta.TEXTO_LIVRE, false, (short) 1);

        mockMvc.perform(post("/api/pergunta/{id}", vagaId)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("PUT /api/pergunta - deve atualizar pergunta e retornar 200")
    @WithMockUser
    void deveAtualizarPerguntaComSucesso() throws Exception {
        UpdatePerguntaDTO dto = new UpdatePerguntaDTO(UUID.randomUUID(), "Nova pergunta?", TipoPergunta.SIM_NAO, true, (short) 2);
        doNothing().when(perguntaService).update(any(UpdatePerguntaDTO.class));

        mockMvc.perform(put("/api/pergunta")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isOk())
                .andExpect(content().string("Pergunta atualizada com sucesso"));
    }

    @Test
    @DisplayName("GET /api/pergunta/{id} - deve retornar perguntas da vaga")
    @WithMockUser
    void deveRetornarPerguntasPorVaga() throws Exception {
        UUID vagaId = UUID.randomUUID();
        when(perguntaService.listarPerguntasPorVaga(vagaId)).thenReturn(List.of());

        mockMvc.perform(get("/api/pergunta/{id}", vagaId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray());
    }

    @Test
    @DisplayName("GET /api/pergunta/{id} - deve retornar 403 sem autenticação")
    void deveRetornar403AoListarSemAutenticacao() throws Exception {
        UUID vagaId = UUID.randomUUID();

        mockMvc.perform(get("/api/pergunta/{id}", vagaId))
                .andExpect(status().isForbidden());
    }
}
