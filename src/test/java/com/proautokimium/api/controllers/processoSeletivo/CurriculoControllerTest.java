package com.proautokimium.api.controllers.processoSeletivo;

import com.proautokimium.api.Infrastructure.repositories.UserRepository;
import com.proautokimium.api.Infrastructure.security.SecurityConfiguration;
import com.proautokimium.api.Infrastructure.security.TokenService;
import com.proautokimium.api.Infrastructure.services.storage.StorageService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.nio.file.Path;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(CurriculoController.class)
@TestPropertySource(properties = {"server.port=0"})
@Import(SecurityConfiguration.class)
class CurriculoControllerTest {

    @Autowired MockMvc mockMvc;
    @MockitoBean StorageService storageService;
    @MockitoBean TokenService tokenService;
    @MockitoBean AuthenticationManager authenticationManager;
    @MockitoBean UserRepository userRepository;

    @Test
    @DisplayName("GET /api/curriculos/{fileName} - deve retornar 404 quando arquivo não existe")
    @WithMockUser
    void deveRetornar404QuandoArquivoNaoExiste() throws Exception {
        String fileName = "curriculo-inexistente.pdf";
        when(storageService.searchFile(fileName))
                .thenReturn(Path.of("/tmp/nonexistent-curriculos", fileName));

        mockMvc.perform(get("/api/curriculos/{fileName}", fileName))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("GET /api/curriculos/{fileName} - deve retornar 403 sem autenticação quando não é rota pública")
    void deveRetornar403SemAutenticacaoEmRotaNaoPublica() throws Exception {
        mockMvc.perform(get("/api/curriculos/arquivo.pdf"))
                .andExpect(status().isForbidden());
    }
}
