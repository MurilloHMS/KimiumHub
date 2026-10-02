package com.proautokimium.api.controllers.humanResources;

import com.proautokimium.api.Infrastructure.repositories.UserRepository;
import com.proautokimium.api.Infrastructure.security.SecurityConfiguration;
import com.proautokimium.api.Infrastructure.security.TokenService;
import com.proautokimium.api.Infrastructure.services.humanResources.MedicalCertificateService;
import com.proautokimium.api.Infrastructure.services.permission.PermissionService;
import com.proautokimium.api.domain.enums.humanResources.MedicalCertificateStatus;
import com.proautokimium.api.domain.enums.humanResources.SubmissionType;
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
 * Quem confere atestado é a tela do RH com ALTERAR; quem reenvia é o dono, pelo
 * portal. Uma tela não faz o papel da outra.
 */
@WebMvcTest(MedicalCertificateController.class)
@TestPropertySource(properties = {"server.port=0"})
@Import(SecurityConfiguration.class)
class MedicalCertificateReviewPermissionTest {

    @Autowired MockMvc mockMvc;

    @MockitoBean MedicalCertificateService service;
    @MockitoBean PermissionService permissionService;
    @MockitoBean UserRepository userRepository;
    @MockitoBean TokenService tokenService;
    @MockitoBean AuthenticationManager authenticationManager;

    private static String url(String action) {
        return "/api/hr/medical-certificates/" + UUID.randomUUID() + "/" + action;
    }

    @Test
    @DisplayName("quem só consulta a tela do RH não confirma nem recusa")
    @WithMockUser(authorities = {"rh/medical-certificates:CONSULTAR"})
    void consultarNaoConfere() throws Exception {
        mockMvc.perform(post(url("receive")).with(csrf())).andExpect(status().isForbidden());
        mockMvc.perform(post(url("reject")).with(csrf())
                .contentType(MediaType.APPLICATION_JSON).content("{\"notes\":\"x\"}"))
                .andExpect(status().isForbidden());
        verifyNoInteractions(service);
    }

    @Test
    @DisplayName("o portal do funcionário não confere atestado, nem o próprio")
    @WithMockUser(authorities = {"documentos/rh/medical-certificates:ALTERAR"})
    void portalNaoConfere() throws Exception {
        mockMvc.perform(post(url("receive")).with(csrf())).andExpect(status().isForbidden());
        verifyNoInteractions(service);
    }

    @Test
    @DisplayName("com ALTERAR no RH, confirmar funciona sem corpo e leva o login de quem confere")
    @WithMockUser(username = "rita", authorities = {"rh/medical-certificates:ALTERAR"})
    void confirmarSemCorpo() throws Exception {
        mockMvc.perform(post(url("receive")).with(csrf())).andExpect(status().isOk());
        verify(service).confirmReceipt(any(), eq(null), eq("rita"));
    }

    @Test
    @DisplayName("reenviar exige o portal com ALTERAR; a tela do RH não reenvia por ninguém")
    @WithMockUser(authorities = {"rh/medical-certificates:ALTERAR", "documentos/rh/medical-certificates:INCLUIR"})
    void rhNaoReenvia() throws Exception {
        mockMvc.perform(multipart(url("resubmit")).file("file", "%PDF".getBytes())
                        .param("submissionType", "FILE").with(csrf()))
                .andExpect(status().isForbidden());
        verifyNoInteractions(service);
    }

    @Test
    @DisplayName("o dono reenvia com o portal")
    @WithMockUser(username = "ana", authorities = {"documentos/rh/medical-certificates:ALTERAR"})
    void donoReenvia() throws Exception {
        mockMvc.perform(multipart(url("resubmit")).file("file", "%PDF".getBytes())
                        .param("submissionType", "FILE").param("comment", "Mais nítido").with(csrf()))
                .andExpect(status().isOk());
        verify(service).resubmit(any(), eq("ana"), eq(SubmissionType.FILE), eq(null), eq("Mais nítido"), any());
    }

    @Test
    @DisplayName("a lista do RH filtra por status")
    @WithMockUser(authorities = {"rh/medical-certificates:CONSULTAR"})
    void filtroPorStatus() throws Exception {
        when(service.listAll(MedicalCertificateStatus.PENDING)).thenReturn(List.of());
        mockMvc.perform(get("/api/hr/medical-certificates?status=PENDING")).andExpect(status().isOk());
        verify(service).listAll(MedicalCertificateStatus.PENDING);
    }
}
