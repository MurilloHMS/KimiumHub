package com.proautokimium.api.controllers;

import com.proautokimium.api.Infrastructure.repositories.UserRepository;
import com.proautokimium.api.Infrastructure.security.SecurityConfiguration;
import com.proautokimium.api.Infrastructure.security.TokenService;
import com.proautokimium.api.Infrastructure.services.permission.PermissionService;
import com.proautokimium.api.Infrastructure.services.reports.guide.GuideLayoutService;
import com.proautokimium.api.Infrastructure.services.reports.guide.GuideReportService;
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

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Quem edita o layout é o Design (CONFIGURAR). Contratos (INCLUIR) gera guia
 * e lê qual versão está em uso, mas não escreve: um layout publicado por
 * engano muda o guia de todo cliente.
 */
@WebMvcTest(GuideLayoutController.class)
@TestPropertySource(properties = {"server.port=0"})
@Import(SecurityConfiguration.class)
class GuideLayoutControllerTest {

    private static final String BASE = "/api/v1/reports/guide/layout";
    private static final String DRAFT = "{\"document\":{\"page\":{}}}";
    private static final String PREVIEW = "{\"document\":{\"page\":{}},\"productIds\":[\"5f0c6c2e-8e8a-4d38-9d1e-2a0f6b6b1a11\"]}";

    @Autowired MockMvc mockMvc;

    @MockitoBean GuideLayoutService layoutService;
    @MockitoBean GuideReportService reportService;
    @MockitoBean PermissionService permissionService;
    @MockitoBean UserRepository userRepository;
    @MockitoBean TokenService tokenService;
    @MockitoBean AuthenticationManager authenticationManager;

    @Test
    @DisplayName("Design (CONFIGURAR) salva o rascunho, com o login dele como autor")
    @WithMockUser(username = "designer", authorities = {"company/guide:CONFIGURAR"})
    void designSalva() throws Exception {
        mockMvc.perform(put(BASE + "/draft").contentType(MediaType.APPLICATION_JSON).content(DRAFT))
                .andExpect(status().isOk());
        verify(layoutService).saveDraft(any(), eq("designer"));
    }

    @Test
    @DisplayName("Contratos (INCLUIR) não salva rascunho")
    @WithMockUser(authorities = {"company/guide:INCLUIR"})
    void contratosNaoSalva() throws Exception {
        mockMvc.perform(put(BASE + "/draft").contentType(MediaType.APPLICATION_JSON).content(DRAFT))
                .andExpect(status().isForbidden());
        verifyNoInteractions(layoutService);
    }

    @Test
    @DisplayName("Contratos (INCLUIR) não publica")
    @WithMockUser(authorities = {"company/guide:INCLUIR", "company/guide:CONSULTAR"})
    void contratosNaoPublica() throws Exception {
        mockMvc.perform(post(BASE + "/publish").contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isForbidden());
        verifyNoInteractions(layoutService);
    }

    @Test
    @DisplayName("Contratos lê qual versão está em uso")
    @WithMockUser(authorities = {"company/guide:INCLUIR"})
    void contratosLe() throws Exception {
        mockMvc.perform(get(BASE)).andExpect(status().isOk());
        mockMvc.perform(get(BASE + "/catalog")).andExpect(status().isOk());
    }

    @Test
    @DisplayName("a prévia do designer vem inline, para abrir no painel e não baixar")
    @WithMockUser(authorities = {"company/guide:CONFIGURAR"})
    void previaInline() throws Exception {
        when(reportService.preview(any())).thenReturn(new byte[]{'%', 'P', 'D', 'F'});

        mockMvc.perform(post(BASE + "/preview").contentType(MediaType.APPLICATION_JSON).content(PREVIEW))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Disposition", "inline; filename=\"previa-do-guia.pdf\""));
    }

    @Test
    @DisplayName("a prévia com layout arbitrário é do Design: Contratos tem a do guia publicado")
    @WithMockUser(authorities = {"company/guide:INCLUIR"})
    void contratosNaoUsaPreviaDoDesigner() throws Exception {
        mockMvc.perform(post(BASE + "/preview").contentType(MediaType.APPLICATION_JSON).content(PREVIEW))
                .andExpect(status().isForbidden());
        verifyNoInteractions(reportService);
    }

    @Test
    @DisplayName("quem não tem a tela não lê nada")
    @WithMockUser(authorities = {"stock/products:CONSULTAR"})
    void semTela() throws Exception {
        mockMvc.perform(get(BASE)).andExpect(status().isForbidden());
    }
}
