package com.proautokimium.api.controllers;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.proautokimium.api.Application.DTOs.permission.PermissionDTOs.GridDTO;
import com.proautokimium.api.Infrastructure.repositories.UserRepository;
import com.proautokimium.api.Infrastructure.security.SecurityConfiguration;
import com.proautokimium.api.Infrastructure.security.TokenService;
import com.proautokimium.api.Infrastructure.services.permission.PermissionAdminService;
import com.proautokimium.api.Infrastructure.services.permission.PermissionService;
import com.proautokimium.api.web.errors.GlobalExceptionHandler;
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

import java.util.Map;
import java.util.UUID;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * O que o 403 conta para quem levou.
 *
 * **É a defesa do passo 5.** Anotar 228 endpoints é irreversível na prática: um
 * mapeamento errado tira o trabalho de alguém no meio do expediente, e o
 * sintoma — "Access Denied" — não diz nem em que tela procurar. O chamado chega
 * como "não consigo salvar" e a investigação começa do zero.
 *
 * Com a authority no corpo, o mesmo chamado chega como
 * "falta stock/movements:EXCLUIR" e acaba numa célula da tela de permissões.
 *
 * Estes testes usam o controller de permissões porque ele é o único anotado
 * hoje — mas o que se prova aqui vale para os 228 que vêm.
 */
@WebMvcTest(PermissionAdminController.class)
@TestPropertySource(properties = {"server.port=0"})
@Import({SecurityConfiguration.class, GlobalExceptionHandler.class})
class ForbiddenMessageTest {

    @Autowired MockMvc mockMvc;
    @Autowired ObjectMapper json;

    @MockitoBean PermissionAdminService service;
    @MockitoBean PermissionService permissionService;
    @MockitoBean UserRepository userRepository;
    @MockitoBean TokenService tokenService;
    @MockitoBean AuthenticationManager authenticationManager;

    /**
     * **O teste que vale por todos.**
     *
     * A authority exigida aparece no corpo, com a tela e a ação separadas. Se
     * esta asserção cair, todo 403 do sistema volta a ser um enigma.
     */
    @Test
    @DisplayName("o 403 diz qual authority faltou")
    @WithMockUser(username = "ricardo", authorities = {"ROLE_USER"})
    void dizQualAuthorityFaltou() throws Exception {
        mockMvc.perform(put("/api/permissions/users/u-1/grid")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json.writeValueAsString(new GridDTO(Map.of()))))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.message")
                        .value(org.hamcrest.Matchers.containsString(
                                "settings/permissions/users:ALTERAR")))
                .andExpect(jsonPath("$.message")
                        .value(org.hamcrest.Matchers.containsString("ALTERAR em")));
    }

    /**
     * Quando o endpoint aceita mais de uma, a frase muda de "Falta" para
     * "Falta uma destas" — senão quem lê tenta conseguir as duas.
     */
    @Test
    @DisplayName("com hasAnyAuthority, o 403 lista as alternativas")
    @WithMockUser(username = "ricardo", authorities = {"ROLE_USER"})
    void listaAlternativas() throws Exception {
        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders
                        .get("/api/permissions/screens"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.message")
                        .value(org.hamcrest.Matchers.containsString("Falta uma destas")))
                .andExpect(jsonPath("$.message")
                        .value(org.hamcrest.Matchers.containsString(
                                "settings/permissions/templates:CONSULTAR")));
    }

    /**
     * Sem login, o 403 vem **sem corpo nenhum** — e isso é o Spring Security,
     * não este handler.
     *
     * A recusa acontece no filtro, antes de qualquer controller, então o
     * `@RestControllerAdvice` nem é chamado. Registro isso aqui porque o
     * contrário é fácil de supor: alguém lê o handler, conclui que todo 403
     * tem `message`, e escreve um front que quebra ao ler `error.message` de
     * um corpo vazio.
     *
     * Na prática não atrapalha — quem não está logado é levado ao login pelo
     * guard, não por esta resposta.
     */
    @Test
    @DisplayName("sem autenticação, o 403 vem sem corpo — quem recusa é o filtro")
    void semLoginNaoTemCorpo() throws Exception {
        mockMvc.perform(post("/api/permissions/templates/" + UUID.randomUUID() + "/apply")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isForbidden())
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers
                        .content().string(""));
    }
}
