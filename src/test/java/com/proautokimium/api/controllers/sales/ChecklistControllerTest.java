package com.proautokimium.api.controllers.sales;

import com.proautokimium.api.Application.DTOs.sales.ChecklistCatalogDTO;
import com.proautokimium.api.Application.DTOs.sales.ChecklistDetailDTO;
import com.proautokimium.api.Application.DTOs.sales.ChecklistSummaryDTO;
import com.proautokimium.api.Infrastructure.repositories.UserRepository;
import com.proautokimium.api.Infrastructure.security.SecurityConfiguration;
import com.proautokimium.api.Infrastructure.security.TokenService;
import com.proautokimium.api.Infrastructure.services.permission.PermissionService;
import com.proautokimium.api.Infrastructure.services.sales.ChecklistCatalogService;
import com.proautokimium.api.Infrastructure.services.sales.ChecklistService;
import com.proautokimium.api.Infrastructure.services.sales.pdf.ChecklistPdfService;
import com.proautokimium.api.domain.entities.sales.Checklist;
import com.proautokimium.api.domain.exceptions.sales.InvalidChecklistException;
import com.proautokimium.api.domain.valueObjects.sales.ChecklistFixtures;
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
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Quem pode o quê. As telas: {@code vendas/checklist} (vendedor) e
 * {@code vendas/checklists} (Controladoria).
 */
@WebMvcTest(ChecklistController.class)
@TestPropertySource(properties = {"server.port=0"})
@Import(SecurityConfiguration.class)
class ChecklistControllerTest {

    @Autowired MockMvc mockMvc;

    @MockitoBean ChecklistService service;
    @MockitoBean ChecklistCatalogService catalogService;
    @MockitoBean ChecklistPdfService pdfService;
    @MockitoBean PermissionService permissionService;
    @MockitoBean UserRepository userRepository;
    @MockitoBean TokenService tokenService;
    @MockitoBean AuthenticationManager authenticationManager;

    static final UUID ID = UUID.fromString("7d7f5a0c-1c2b-4c8e-9a51-2f7b3f7e0a11");

    static final String BODY = """
            {"revision":1,"filledOffline":true,"content":{"customer":{"name":"Mercado"}}}
            """;

    static ChecklistDetailDTO detail() {
        Checklist c = Checklist.submit(ID, "diego", "Diego", ChecklistFixtures.valid(), false, null,
                LocalDateTime.of(2026, 9, 30, 10, 0));
        return new ChecklistDetailDTO(ChecklistSummaryDTO.from(c), c.getContent(), List.of(), List.of(), List.of());
    }

    @Test
    @DisplayName("vendedor envia com INCLUIR")
    @WithMockUser(username = "diego", authorities = "vendas/checklist:INCLUIR")
    void sellerSubmits() throws Exception {
        when(service.submit(eq(ID), any(), eq("diego"))).thenReturn(detail());

        mockMvc.perform(put("/api/checklists/" + ID).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON).content(BODY))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.summary.status").value("SUBMITTED"));
    }

    @Test
    @DisplayName("sem a tela do vendedor, não envia")
    @WithMockUser(authorities = "vendas/checklists:CONSULTAR")
    void submitNeedsSellerScreen() throws Exception {
        mockMvc.perform(put("/api/checklists/" + ID).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON).content(BODY))
                .andExpect(status().isForbidden());
        verifyNoInteractions(service);
    }

    @Test
    @DisplayName("checklist incompleto volta 400 com o que falta, para a tela mostrar")
    @WithMockUser(username = "diego", authorities = "vendas/checklist:INCLUIR")
    void invalidIs400() throws Exception {
        when(service.submit(eq(ID), any(), anyString()))
                .thenThrow(new InvalidChecklistException("Etapa 3 — o CPF de quem assina é inválido."));

        mockMvc.perform(put("/api/checklists/" + ID).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON).content(BODY))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Etapa 3 — o CPF de quem assina é inválido."));
    }

    @Test
    @DisplayName("vendedor não lista todos, nem aprova")
    @WithMockUser(authorities = {"vendas/checklist:INCLUIR", "vendas/checklist:CONSULTAR"})
    void sellerIsNotReviewer() throws Exception {
        mockMvc.perform(get("/api/checklists")).andExpect(status().isForbidden());
        mockMvc.perform(post("/api/checklists/" + ID + "/approve").with(csrf())
                        .contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isForbidden());
        verifyNoInteractions(service);
    }

    @Test
    @DisplayName("abrir um checklist: a Controladoria vê qualquer um; o vendedor, os seus")
    @WithMockUser(username = "fernanda", authorities = "vendas/checklists:CONSULTAR")
    void reviewerFlag() throws Exception {
        when(service.detail(ID, "fernanda", true)).thenReturn(detail());
        mockMvc.perform(get("/api/checklists/" + ID)).andExpect(status().isOk());
        verify(service).detail(ID, "fernanda", true);
    }

    @Test
    @DisplayName("vendedor abre com reviewer = false")
    @WithMockUser(username = "diego", authorities = "vendas/checklist:CONSULTAR")
    void sellerFlag() throws Exception {
        when(service.detail(ID, "diego", false)).thenReturn(detail());
        mockMvc.perform(get("/api/checklists/" + ID)).andExpect(status().isOk());
        verify(service).detail(ID, "diego", false);
    }

    @Test
    @DisplayName("catálogo: mesmo ETag devolve 304, sem corpo")
    @WithMockUser(authorities = "vendas/checklist:INCLUIR")
    void catalogEtag() throws Exception {
        when(catalogService.catalog()).thenReturn(new ChecklistCatalogDTO("abc123", null, List.of(), List.of(),
                List.of(), List.of(), List.of()));

        // Fraco (W/), senão o Tomcat não comprime os 3 MB do catálogo.
        mockMvc.perform(get("/api/checklists/catalog"))
                .andExpect(status().isOk())
                .andExpect(header().string("ETag", "W/\"abc123\""));
        mockMvc.perform(get("/api/checklists/catalog").header("If-None-Match", "W/\"abc123\""))
                .andExpect(status().isNotModified())
                .andExpect(content().string(""));
        // Quem guardou sem o W/ também recebe 304.
        mockMvc.perform(get("/api/checklists/catalog").header("If-None-Match", "\"abc123\""))
                .andExpect(status().isNotModified());
    }

    @Test
    @DisplayName("PDF: a Controladoria baixa qualquer um")
    @WithMockUser(username = "fernanda", authorities = "vendas/checklists:BAIXAR")
    void pdf() throws Exception {
        Checklist c = Checklist.submit(ID, "diego", "Diego", ChecklistFixtures.valid(), false, null,
                LocalDateTime.of(2026, 9, 30, 10, 0));
        when(service.findVisible(ID, "fernanda", true)).thenReturn(c);
        when(pdfService.generate(c)).thenReturn(new byte[]{'%', 'P', 'D', 'F'});

        mockMvc.perform(get("/api/checklists/" + ID + "/pdf"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_PDF));
    }
}
