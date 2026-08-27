package com.proautokimium.api.controllers;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.proautokimium.api.Application.DTOs.permission.PermissionDTOs.*;
import com.proautokimium.api.Infrastructure.repositories.UserRepository;
import com.proautokimium.api.Infrastructure.security.SecurityConfiguration;
import com.proautokimium.api.Infrastructure.security.TokenService;
import com.proautokimium.api.Infrastructure.services.permission.PermissionAdminService;
import com.proautokimium.api.Infrastructure.services.permission.PermissionService;
import com.proautokimium.api.domain.enums.ApplyMode;
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
import java.util.Map;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * **A porta que não pode ficar aberta um minuto.**
 *
 * Os outros 214 endpoints esperam o passo 5 para ganhar `@PreAuthorize`, e a
 * espera é aceitável: o pior que acontece é alguém ler o que não devia. Aqui
 * não — quem alcança `PUT /users/{id}/grid` **se dá tudo**, e depois disso todo
 * o resto do sistema está aberto para essa pessoa.
 *
 * Por isso o teste que importa neste arquivo não é o do caminho feliz. É o de
 * quem está logado, é funcionário, e **mesmo assim leva 403**.
 */
@WebMvcTest(PermissionAdminController.class)
@TestPropertySource(properties = {"server.port=0"})
@Import(SecurityConfiguration.class)
class PermissionAdminControllerTest {

    private static final String TEMPLATES = "settings/permissions/templates";
    private static final String USERS = "settings/permissions/users";

    @Autowired MockMvc mockMvc;
    @Autowired ObjectMapper json;

    @MockitoBean PermissionAdminService service;
    @MockitoBean PermissionService permissionService;
    @MockitoBean UserRepository userRepository;
    @MockitoBean TokenService tokenService;
    @MockitoBean AuthenticationManager authenticationManager;

    // ─── O que este arquivo existe para provar ───────────────────────────────

    /**
     * O funcionário logado sem a permissão de configuração.
     *
     * É o estado de **todo mundo** depois da V87, menos o admin. Se este teste
     * cair, qualquer pessoa da empresa pode se dar todas as telas com um
     * `curl` — e o front escondendo o menu não muda nada disso.
     */
    @Test
    @DisplayName("funcionário sem a permissão não grava a grade de ninguém")
    @WithMockUser(username = "ricardo", authorities = {"ROLE_USER", "stock/movements:CONSULTAR"})
    void funcionarioComumNaoGravaPermissao() throws Exception {
        mockMvc.perform(put("/api/permissions/users/u-1/grid")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json.writeValueAsString(new GridDTO(Map.of()))))
                .andExpect(status().isForbidden());

        verifyNoInteractions(service);
    }

    @Test
    @DisplayName("funcionário sem a permissão não aplica modelo em ninguém")
    @WithMockUser(username = "ricardo", authorities = {"ROLE_USER"})
    void funcionarioComumNaoAplica() throws Exception {
        mockMvc.perform(post("/api/permissions/templates/" + UUID.randomUUID() + "/apply")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json.writeValueAsString(
                                new ApplyTemplateDTO(List.of("u-1"), ApplyMode.SUBSTITUIR))))
                .andExpect(status().isForbidden());

        verifyNoInteractions(service);
    }

    @Test
    @DisplayName("sem autenticação, nada")
    void semLoginNada() throws Exception {
        mockMvc.perform(get("/api/permissions/screens"))
                .andExpect(status().isForbidden());

        verifyNoInteractions(service);
    }

    /**
     * **Consultar não é alterar**, nem aqui.
     *
     * Quem só enxerga a configuração não pode mudá-la. Sem este teste, a
     * diferença entre as duas anotações some no dia em que alguém copiar e
     * colar um método.
     */
    @Test
    @DisplayName("quem só consulta não grava")
    @WithMockUser(username = "ana", authorities = {USERS + ":CONSULTAR"})
    void consultarNaoGrava() throws Exception {
        mockMvc.perform(put("/api/permissions/users/u-1/grid")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json.writeValueAsString(new GridDTO(Map.of()))))
                .andExpect(status().isForbidden());
    }

    /**
     * **Alterar não é aplicar modelo.**
     *
     * Alterar mexe numa pessoa; aplicar alcança várias de uma vez, e no modo
     * SUBSTITUIR apaga ajuste individual de todas elas. São dois pesos, e por
     * isso duas permissões.
     */
    @Test
    @DisplayName("quem altera uma pessoa não aplica um modelo em várias")
    @WithMockUser(username = "ana", authorities = {USERS + ":ALTERAR"})
    void alterarNaoAplica() throws Exception {
        mockMvc.perform(post("/api/permissions/templates/" + UUID.randomUUID() + "/apply")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json.writeValueAsString(
                                new ApplyTemplateDTO(List.of("u-1"), ApplyMode.SOMAR))))
                .andExpect(status().isForbidden());
    }

    /**
     * Desfazer tira acesso, então pesa o mesmo que aplicar.
     *
     * Quem só altera uma pessoa por vez não pode desfazer um modelo inteiro
     * nela — é o mesmo raciocínio da aplicação em massa, na direção contrária.
     */
    @Test
    @DisplayName("quem só altera não desfaz a aplicação de um modelo")
    @WithMockUser(username = "ana", authorities = {USERS + ":ALTERAR"})
    void alterarNaoDesfaz() throws Exception {
        mockMvc.perform(delete("/api/permissions/users/u-1/templates/" + UUID.randomUUID())
                        .with(csrf()))
                .andExpect(status().isForbidden());

        verifyNoInteractions(service);
    }

    @Test
    @DisplayName("com CONFIGURAR, desfazer responde quantas células saíram")
    @WithMockUser(username = "murillo", authorities = {USERS + ":CONFIGURAR"})
    void desfazerResponde() throws Exception {
        when(service.undoApply(anyString(), any())).thenReturn(new ApplyResultDTO(1, 9));

        mockMvc.perform(delete("/api/permissions/users/u-1/templates/" + UUID.randomUUID())
                        .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.cellsChanged").value(9));
    }

    // ─── O catálogo serve às duas telas ──────────────────────────────────────

    /**
     * O catálogo abre com **qualquer uma** das duas permissões.
     *
     * Exigir a de modelos para desenhar a grade de um usuário trancaria quem só
     * cuida de pessoas — e o sintoma seria uma tela de configuração sem linha
     * nenhuma, que ninguém associa a permissão.
     */
    @Test
    @DisplayName("quem só cuida de pessoas ainda enxerga o catálogo de telas")
    @WithMockUser(username = "ana", authorities = {USERS + ":CONSULTAR"})
    void catalogoAbreComQualquerUmaDasDuas() throws Exception {
        when(service.screens()).thenReturn(List.of(
                new ScreenDTO("stock/movements", "Movimentações", "Estoque", 220)));

        mockMvc.perform(get("/api/permissions/screens"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].code").value("stock/movements"));
    }

    // ─── Caminho feliz ───────────────────────────────────────────────────────

    @Test
    @DisplayName("com a permissão, a grade grava e responde quantas células mudaram")
    @WithMockUser(username = "murillo", authorities = {USERS + ":ALTERAR"})
    void comPermissaoGrava() throws Exception {
        when(service.saveUserGrid(anyString(), any())).thenReturn(12);

        mockMvc.perform(put("/api/permissions/users/u-1/grid")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json.writeValueAsString(
                                new GridDTO(Map.of("stock/movements", List.of("CONSULTAR"))))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.cellsChanged").value(12));
    }

    /**
     * Quem aplicou fica registrado, e vem do token — não do corpo.
     *
     * Se viesse do corpo, o registro de "quem aplicou" seria escrito por quem
     * aplicou, e não valeria nada numa auditoria.
     */
    @Test
    @DisplayName("a aplicação registra quem aplicou, tirado da autenticação")
    @WithMockUser(username = "murillo", authorities = {USERS + ":CONFIGURAR"})
    void aplicacaoRegistraQuemAplicou() throws Exception {
        UUID modelo = UUID.randomUUID();
        when(service.apply(any(), any(), org.mockito.ArgumentMatchers.eq("murillo")))
                .thenReturn(new ApplyResultDTO(2, 40));

        mockMvc.perform(post("/api/permissions/templates/" + modelo + "/apply")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json.writeValueAsString(
                                new ApplyTemplateDTO(List.of("u-1", "u-2"), ApplyMode.SOMAR))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.users").value(2))
                .andExpect(jsonPath("$.cellsChanged").value(40));
    }
}
