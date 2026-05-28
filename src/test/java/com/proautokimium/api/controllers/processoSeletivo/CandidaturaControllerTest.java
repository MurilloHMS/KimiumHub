package com.proautokimium.api.controllers.processoSeletivo;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.proautokimium.api.Application.DTOs.processoSeletivo.candidaturas.CreateCandidaturaDTO;
import com.proautokimium.api.Application.DTOs.processoSeletivo.candidaturas.ResponseCandidaturaDTO;
import com.proautokimium.api.Infrastructure.exceptions.processoSeletivo.CandidaturaAlreadyExistsException;
import com.proautokimium.api.Infrastructure.repositories.UserRepository;
import com.proautokimium.api.Infrastructure.security.SecurityConfiguration;
import com.proautokimium.api.Infrastructure.security.TokenService;
import com.proautokimium.api.Infrastructure.services.processoSeletivo.CandidaturaService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
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

@WebMvcTest(CandidaturaController.class)
@TestPropertySource(properties = {
        "server.port=0"
})
@Import(SecurityConfiguration.class)
class CandidaturaControllerTest {

    @Autowired MockMvc mockMvc;
    @Autowired ObjectMapper objectMapper;
    @MockitoBean CandidaturaService candidaturaService;
    @MockitoBean private TokenService tokenService;
    @MockitoBean private AuthenticationManager authenticationManager;
    @MockitoBean private UserRepository userRepository;

    @Test
    @DisplayName("GET /api/candidatura/{id} - deve retornar candidaturas da vaga")
    @WithMockUser(roles = "ADMIN")
    void deveRetornarCandidaturasDaVaga() throws Exception {
        UUID vagaId = UUID.randomUUID();
        ResponseCandidaturaDTO dto = mock(ResponseCandidaturaDTO.class);
        when(candidaturaService.getCandidaturaByVagaId(vagaId)).thenReturn(List.of(dto));

        mockMvc.perform(get("/api/candidatura/{id}", vagaId)
                        .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray());
    }

    @Test
    @DisplayName("POST /api/candidatura - deve criar candidatura com currículo")
    @WithMockUser(roles = "ADMIN")
    void deveCriarCandidaturaComCurriculo() throws Exception {
        CreateCandidaturaDTO dto = new CreateCandidaturaDTO(UUID.randomUUID(),
                "João", "joao@email.com", "11999999999",
                "linkedin.com/in/joao"
        );

        MockMultipartFile dados = new MockMultipartFile(
                "dados", "", MediaType.APPLICATION_JSON_VALUE,
                objectMapper.writeValueAsBytes(dto)
        );
        MockMultipartFile curriculo = new MockMultipartFile(
                "curriculo", "cv.pdf", "application/pdf", "conteudo".getBytes()
        );

        doNothing().when(candidaturaService).create(any(), any());

        mockMvc.perform(multipart("/api/candidatura")
                        .file(dados)
                        .file(curriculo)
                        .contentType(MediaType.MULTIPART_FORM_DATA_VALUE)
                        .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(content().string("Candidatura realizada com sucesso"));
    }

    @Test
    @DisplayName("POST /api/candidatura - deve retornar 4xx se candidatura duplicada")
    @WithMockUser(roles = "ADMIN")
    void deveRetornarErroCandidaturaDuplicada() throws Exception {
        CreateCandidaturaDTO dto = new CreateCandidaturaDTO(UUID.randomUUID(),
                "João", "joao@email.com", "11999999999",
                "linkedin.com/in/joao"
        );

        MockMultipartFile dados = new MockMultipartFile(
                "dados", "", MediaType.APPLICATION_JSON_VALUE,
                objectMapper.writeValueAsBytes(dto)
        );

        doThrow(new CandidaturaAlreadyExistsException("Já se candidatou"))
                .when(candidaturaService).create(any(), any());

        mockMvc.perform(multipart("/api/candidatura")
                        .file(dados)
                        .contentType(MediaType.MULTIPART_FORM_DATA_VALUE)
                        .with(csrf()))
                .andExpect(status().is4xxClientError());
    }

    @Test
    @DisplayName("PUT /api/candidatura/{id}/avancar - deve avançar etapa")
    @WithMockUser(roles = "ADMIN")
    void deveAvancarEtapa() throws Exception {
        UUID id = UUID.randomUUID();
        doNothing().when(candidaturaService).avancarEtapa(id);

        mockMvc.perform(put("/api/candidatura/{id}/avancar", id)
                        .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(content().string("Etapa avançada com sucesso"));
    }

    @Test
    @DisplayName("PUT /api/candidatura/{id}/aprovar - deve aprovar candidatura")
    @WithMockUser(roles = "ADMIN")
    void deveAprovarCandidatura() throws Exception {
        UUID id = UUID.randomUUID();
        doNothing().when(candidaturaService).aprovarCandidatura(id);

        mockMvc.perform(put("/api/candidatura/{id}/aprovar", id)
                        .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(content().string("Candidatura aprovada com sucesso"));
    }

    @Test
    @DisplayName("PUT /api/candidatura/{id}/reprovar - deve reprovar candidatura")
    @WithMockUser(roles = "ADMIN")
    void deveReprovarCandidatura() throws Exception {
        UUID id = UUID.randomUUID();
        doNothing().when(candidaturaService).reprovarCandidatura(id);

        mockMvc.perform(put("/api/candidatura/{id}/reprovar", id)
                        .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(content().string("Candidatura reprovada com sucesso"));
    }

    @Test
    @DisplayName("PUT /api/candidatura/{id}/encerrar - deve encerrar candidatura")
    @WithMockUser(roles = "ADMIN")
    void deveEncerrarCandidatura() throws Exception {
        UUID id = UUID.randomUUID();
        doNothing().when(candidaturaService).encerrarCandidatura(id);

        mockMvc.perform(put("/api/candidatura/{id}/encerrar", id)
                        .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(content().string("Candidatura encerrada com sucesso"));
    }
}