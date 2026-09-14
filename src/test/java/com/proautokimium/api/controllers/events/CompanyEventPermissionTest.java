package com.proautokimium.api.controllers.events;

import com.proautokimium.api.Infrastructure.repositories.UserRepository;
import com.proautokimium.api.Infrastructure.security.SecurityConfiguration;
import com.proautokimium.api.Infrastructure.security.TokenService;
import com.proautokimium.api.Infrastructure.services.events.CompanyEventService;
import com.proautokimium.api.Infrastructure.services.events.SpeakerService;
import com.proautokimium.api.Infrastructure.services.permission.PermissionService;
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

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * As duas telas dos eventos: {@code documentos/eventos} só vê;
 * {@code communication/events} cadastra. As regras que quebram tela sem erro são
 * as de fronteira entre as duas.
 */
@WebMvcTest({CompanyEventController.class, SpeakerController.class})
@TestPropertySource(properties = {"server.port=0"})
@Import(SecurityConfiguration.class)
class CompanyEventPermissionTest {

    @Autowired MockMvc mockMvc;

    @MockitoBean CompanyEventService eventService;
    @MockitoBean SpeakerService speakerService;
    @MockitoBean PermissionService permissionService;
    @MockitoBean UserRepository userRepository;
    @MockitoBean TokenService tokenService;
    @MockitoBean AuthenticationManager authenticationManager;

    private static final String EVENTO = """
            {"name":"Poseidon Week","startDate":"2026-09-22","endDate":"2026-09-25"}
            """;

    @Test
    @DisplayName("quem so tem Documentos ve os eventos publicados")
    @WithMockUser(authorities = {"documentos/eventos:CONSULTAR"})
    void documentosVe() throws Exception {
        when(eventService.listPublished()).thenReturn(List.of());

        mockMvc.perform(get("/api/events")).andExpect(status().isOk());
    }

    @Test
    @DisplayName("quem so tem Documentos nao lista o cadastro, que tem rascunhos")
    @WithMockUser(authorities = {"documentos/eventos:CONSULTAR"})
    void documentosNaoListaCadastro() throws Exception {
        mockMvc.perform(get("/api/events/manage")).andExpect(status().isForbidden());

        verifyNoInteractions(eventService);
    }

    /**
     * O rascunho abre ou não pela authority de quem pede, e nunca por parâmetro
     * da URL: um {@code ?drafts=true} seria o jeito de qualquer um espiar.
     */
    @Test
    @DisplayName("Documentos pede o evento sem direito a rascunho")
    @WithMockUser(authorities = {"documentos/eventos:CONSULTAR"})
    void documentosSemRascunho() throws Exception {
        UUID id = UUID.randomUUID();

        mockMvc.perform(get("/api/events/{id}", id).param("drafts", "true")).andExpect(status().isOk());

        verify(eventService).get(id, false);
    }

    @Test
    @DisplayName("quem cadastra pede o evento com direito a rascunho, para o Ver como fica")
    @WithMockUser(authorities = {"communication/events:CONSULTAR"})
    void cadastroComRascunho() throws Exception {
        UUID id = UUID.randomUUID();

        mockMvc.perform(get("/api/events/{id}", id)).andExpect(status().isOk());

        verify(eventService).get(id, true);
    }

    @Test
    @DisplayName("ver nao cria evento")
    @WithMockUser(authorities = {"documentos/eventos:CONSULTAR", "communication/events:CONSULTAR"})
    void verNaoCria() throws Exception {
        mockMvc.perform(multipart("/api/events")
                        .file(new MockMultipartFile("data", "", MediaType.APPLICATION_JSON_VALUE, EVENTO.getBytes(StandardCharsets.UTF_8)))
                        .with(csrf()))
                .andExpect(status().isForbidden());

        verifyNoInteractions(eventService);
    }

    @Test
    @DisplayName("com INCLUIR, cria e assina com o login de quem pediu")
    @WithMockUser(username = "murillo.henrique", authorities = {"communication/events:INCLUIR"})
    void criaComAutor() throws Exception {
        mockMvc.perform(multipart("/api/events")
                        .file(new MockMultipartFile("data", "", MediaType.APPLICATION_JSON_VALUE, EVENTO.getBytes(StandardCharsets.UTF_8)))
                        .with(csrf()))
                .andExpect(status().isOk());

        verify(eventService).create(any(), eq(null), eq("murillo.henrique"));
    }

    @Test
    @DisplayName("publicar pede ALTERAR, e nao INCLUIR")
    @WithMockUser(authorities = {"communication/events:INCLUIR"})
    void publicarPedeAlterar() throws Exception {
        mockMvc.perform(post("/api/events/{id}/publish", UUID.randomUUID()).with(csrf()))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("a lista de palestrantes e do cadastro, nao de Documentos")
    @WithMockUser(authorities = {"documentos/eventos:CONSULTAR"})
    void palestrantesSoNoCadastro() throws Exception {
        mockMvc.perform(get("/api/speakers")).andExpect(status().isForbidden());

        verifyNoInteractions(speakerService);
    }
}
