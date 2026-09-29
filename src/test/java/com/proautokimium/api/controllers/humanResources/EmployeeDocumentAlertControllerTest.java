package com.proautokimium.api.controllers.humanResources;

import com.proautokimium.api.Infrastructure.repositories.UserRepository;
import com.proautokimium.api.Infrastructure.security.SecurityConfiguration;
import com.proautokimium.api.Infrastructure.security.TokenService;
import com.proautokimium.api.Infrastructure.services.humanResources.EmployeeDocumentAlertService;
import com.proautokimium.api.Infrastructure.services.permission.PermissionService;
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

import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * "Rodar os avisos agora" dispara e-mail de verdade: só quem configura os
 * tipos pode apertar. Quem só consulta a tela não.
 */
@WebMvcTest(EmployeeDocumentAlertController.class)
@TestPropertySource(properties = {"server.port=0"})
@Import(SecurityConfiguration.class)
class EmployeeDocumentAlertControllerTest {

    @Autowired MockMvc mockMvc;

    @MockitoBean EmployeeDocumentAlertService service;
    @MockitoBean PermissionService permissionService;
    @MockitoBean UserRepository userRepository;
    @MockitoBean TokenService tokenService;
    @MockitoBean AuthenticationManager authenticationManager;

    @Test
    @DisplayName("quem configura roda, e recebe quantos avisaram")
    @WithMockUser(authorities = "rh/employee-documents:CONFIGURAR")
    void configuraRoda() throws Exception {
        when(service.runAlerts()).thenReturn(3);

        mockMvc.perform(post("/api/hr/employee-document-alerts/run").with(
                        org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.alerted").value(3));
    }

    @Test
    @DisplayName("quem só consulta a tela não roda")
    @WithMockUser(authorities = "rh/employee-documents:CONSULTAR")
    void consultaNaoRoda() throws Exception {
        mockMvc.perform(post("/api/hr/employee-document-alerts/run")).andExpect(status().isForbidden());
        verify(service, never()).runAlerts();
    }
}
