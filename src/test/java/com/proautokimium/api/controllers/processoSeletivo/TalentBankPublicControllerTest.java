package com.proautokimium.api.controllers.processoSeletivo;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.proautokimium.api.Application.DTOs.processoSeletivo.talentBank.TalentBankEntryDTO;
import com.proautokimium.api.Infrastructure.repositories.UserRepository;
import com.proautokimium.api.Infrastructure.security.SecurityConfiguration;
import com.proautokimium.api.Infrastructure.security.TokenService;
import com.proautokimium.api.Infrastructure.services.permission.PermissionService;
import com.proautokimium.api.Infrastructure.services.processoSeletivo.TalentBankService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.nio.charset.StandardCharsets;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * As seis rotas públicas do banco de talentos, sem ninguém logado.
 *
 * <p><b>Este arquivo existe por causa de um buraco estrutural.</b> Até
 * 2026-09-11 o projeto só tinha lista pública para GET e POST — não existiam
 * {@code PUBLIC_PUT} nem {@code PUBLIC_DELETE}, e qualquer outro verbo caía no
 * {@code anyRequest()}. O {@code PublicPathsHaveNoPreAuthorizeTest} varre por
 * verbo, e o método dele só conhecia GET e POST: anotar o PUT novo por engano
 * deixaria a suíte <b>verde</b> e quebraria o formulário no deploy.
 *
 * <p>Aquele teste foi estendido no mesmo commit. Este aqui cobre o outro lado:
 * que os caminhos realmente passam pelo filtro sem autenticação.
 */
@WebMvcTest(TalentBankPublicController.class)
@TestPropertySource(properties = {"server.port=0"})
@Import(SecurityConfiguration.class)
class TalentBankPublicControllerTest {

    @Autowired MockMvc mockMvc;
    @Autowired ObjectMapper objectMapper;

    @MockitoBean TalentBankService talentBankService;
    @MockitoBean TokenService tokenService;
    @MockitoBean PermissionService permissionService;
    @MockitoBean AuthenticationManager authenticationManager;
    @MockitoBean UserRepository userRepository;

    private static MockMultipartFile dados(String json) {
        return new MockMultipartFile("dados", "", MediaType.APPLICATION_JSON_VALUE,
                json.getBytes(StandardCharsets.UTF_8));
    }

    private static MockMultipartFile curriculo() {
        return new MockMultipartFile("curriculo", "cv.pdf", MediaType.APPLICATION_PDF_VALUE,
                "%PDF-1.7".getBytes(StandardCharsets.UTF_8));
    }

    private static final String INSCRICAO = """
            {"nome":"Maria Souza","email":"maria@email.com","telefone":"44999990000",
             "urlLinkedin":null,"areaInteresse":"Produção","consentimento":true}
            """;

    // ─── As seis, anonimamente ───────────────────────────────────────────────

    @Test
    @DisplayName("POST inscricao responde 202 sem autenticacao")
    void inscricaoEPublica() throws Exception {
        mockMvc.perform(multipart("/api/talent-bank/public")
                        .file(dados(INSCRICAO))
                        .file(curriculo())
                        .with(csrf()))
                .andExpect(status().isAccepted());
    }

    @Test
    @DisplayName("POST access-link responde 202 sem autenticacao")
    void pedirLinkEPublico() throws Exception {
        mockMvc.perform(post("/api/talent-bank/public/access-link")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"maria@email.com\"}"))
                .andExpect(status().isAccepted());
    }

    @Test
    @DisplayName("GET por token e publico")
    void verEPublico() throws Exception {
        when(talentBankService.verPorToken("tok")).thenReturn(
                new TalentBankEntryDTO("Maria", "maria@email.com", "44999990000", null,
                        "Produção", true, "pdf", null, null, null, null, List.of()));

        mockMvc.perform(get("/api/talent-bank/public/tok"))
                .andExpect(status().isOk());
    }

    /**
     * <b>O verbo que não tinha lista pública no projeto.</b> Sem
     * {@code PUBLIC_PUT} no {@code SecurityPaths} <i>e</i> a linha no
     * {@code SecurityConfiguration}, isto responde 403 para todo mundo.
     */
    @Test
    @DisplayName("PUT por token e publico")
    void atualizarEPublico() throws Exception {
        when(talentBankService.atualizarPorToken(anyString(), any(), any())).thenReturn(
                new TalentBankEntryDTO("Maria", "maria@email.com", "44999990000", null,
                        null, false, null, null, null, null, null, List.of()));

        var requisicao = multipart("/api/talent-bank/public/tok")
                .file(dados("{\"nome\":\"Maria\",\"telefone\":\"44999990000\"}"))
                .with(csrf());
        requisicao.with(r -> { r.setMethod("PUT"); return r; });

        mockMvc.perform(requisicao).andExpect(status().isOk());
    }

    /** O outro verbo sem lista pública até agora. */
    @Test
    @DisplayName("DELETE por token e publico e responde 204")
    void excluirEPublico() throws Exception {
        mockMvc.perform(delete("/api/talent-bank/public/tok").with(csrf()))
                .andExpect(status().isNoContent());

        verify(talentBankService).excluirPorToken("tok");
    }

    // ─── O que a validação recusa ────────────────────────────────────────────

    /**
     * 400 é sobre o <b>formato</b> do campo. Se o endereço existe ou não na
     * base, a resposta continua 202 — e essa diferença é a linha entre um
     * formulário e um oráculo de enumeração.
     */
    @Test
    @DisplayName("E-mail malformado no access-link da 400")
    void emailMalformadoDa400() throws Exception {
        mockMvc.perform(post("/api/talent-bank/public/access-link")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"nao-e-email\"}"))
                .andExpect(status().isBadRequest());

        verify(talentBankService, org.mockito.Mockito.never()).pedirLinkDeAcesso(anyString());
    }

    @Test
    @DisplayName("Inscricao sem marcar o consentimento da 400 e nao chega ao servico")
    void semConsentimentoDa400() throws Exception {
        String semAceite = INSCRICAO.replace("\"consentimento\":true", "\"consentimento\":false");

        mockMvc.perform(multipart("/api/talent-bank/public")
                        .file(dados(semAceite))
                        .file(curriculo())
                        .with(csrf()))
                .andExpect(status().isBadRequest());

        verify(talentBankService, org.mockito.Mockito.never()).inscrever(any(), any());
    }

    @Test
    @DisplayName("A resposta do access-link nao diz se o e-mail existe")
    void respostaNaoRevelaExistencia() throws Exception {
        mockMvc.perform(post("/api/talent-bank/public/access-link")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"quem-sabe@email.com\"}"))
                .andExpect(status().isAccepted())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Se este e-mail estiver")));
    }
}
