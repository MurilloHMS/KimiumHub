package com.proautokimium.api.controllers.humanResources;

import com.proautokimium.api.Application.DTOs.humanResources.ReportRecipient.HrReportRecipientDTO;
import com.proautokimium.api.Infrastructure.repositories.UserRepository;
import com.proautokimium.api.Infrastructure.security.SecurityConfiguration;
import com.proautokimium.api.Infrastructure.security.TokenService;
import com.proautokimium.api.Infrastructure.services.humanResources.HrReportRecipientService;
import com.proautokimium.api.Infrastructure.services.permission.PermissionService;
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

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Ler a lista é CONSULTAR; mudar para onde vão os relatórios é CONFIGURAR. */
@WebMvcTest(HrReportRecipientController.class)
@TestPropertySource(properties = {"server.port=0"})
@Import(SecurityConfiguration.class)
class HrReportRecipientControllerTest {

    @Autowired MockMvc mockMvc;

    @MockitoBean HrReportRecipientService service;
    @MockitoBean PermissionService permissionService;
    @MockitoBean UserRepository userRepository;
    @MockitoBean TokenService tokenService;
    @MockitoBean AuthenticationManager authenticationManager;

    @Test
    @DisplayName("quem consulta a tela de reembolsos vê a lista")
    @WithMockUser(authorities = {"rh/reimbursements:CONSULTAR"})
    void consultaLista() throws Exception {
        when(service.list()).thenReturn(List.of());
        mockMvc.perform(get("/api/hr/report-recipients")).andExpect(status().isOk());
    }

    @Test
    @DisplayName("quem só consulta não muda a lista")
    @WithMockUser(authorities = {"rh/reimbursements:CONSULTAR"})
    void consultarNaoAdiciona() throws Exception {
        mockMvc.perform(post("/api/hr/report-recipients").with(csrf())
                        .contentType(MediaType.APPLICATION_JSON).content("{\"email\":\"rh@x.com\"}"))
                .andExpect(status().isForbidden());
        verifyNoInteractions(service);
    }

    @Test
    @DisplayName("com CONFIGURAR, adiciona com o login de quem cadastrou")
    @WithMockUser(username = "carla.rh", authorities = {"rh/reimbursements:CONFIGURAR"})
    void configurarAdiciona() throws Exception {
        when(service.add("rh@x.com", "carla.rh")).thenReturn(
                new HrReportRecipientDTO(UUID.randomUUID(), "rh@x.com", LocalDateTime.now(), "carla.rh"));

        mockMvc.perform(post("/api/hr/report-recipients").with(csrf())
                        .contentType(MediaType.APPLICATION_JSON).content("{\"email\":\"rh@x.com\"}"))
                .andExpect(status().isCreated());
        verify(service).add("rh@x.com", "carla.rh");
    }

    @Test
    @DisplayName("e-mail em branco é 400 com a mensagem do campo")
    @WithMockUser(authorities = {"rh/reimbursements:CONFIGURAR"})
    void emBranco() throws Exception {
        mockMvc.perform(post("/api/hr/report-recipients").with(csrf())
                        .contentType(MediaType.APPLICATION_JSON).content("{\"email\":\"  \"}"))
                .andExpect(status().isBadRequest());
        verifyNoInteractions(service);
    }
}
