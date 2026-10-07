package com.proautokimium.api.controllers.dev;

import com.proautokimium.api.Infrastructure.repositories.UserRepository;
import com.proautokimium.api.Infrastructure.security.SecurityConfiguration;
import com.proautokimium.api.Infrastructure.security.TokenService;
import com.proautokimium.api.Infrastructure.services.email.EmailQueueAdminService;
import com.proautokimium.api.Infrastructure.services.email.EmailSenderAdminService;
import com.proautokimium.api.Infrastructure.services.permission.PermissionService;
import com.proautokimium.api.domain.enums.email.EmailOrigin;
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

import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** As duas telas do desenvolvedor: quem só consulta não reenvia nem muda remetente. */
@WebMvcTest({EmailQueueAdminController.class, EmailSenderAdminController.class})
@TestPropertySource(properties = {"server.port=0"})
@Import(SecurityConfiguration.class)
class DevEmailPermissionTest {

    @Autowired MockMvc mockMvc;
    @MockitoBean EmailQueueAdminService queue;
    @MockitoBean com.proautokimium.api.Infrastructure.services.email.EmailQueueInsightsService insights;
    @MockitoBean EmailSenderAdminService senders;
    @MockitoBean PermissionService permissionService;
    @MockitoBean UserRepository userRepository;
    @MockitoBean TokenService tokenService;
    @MockitoBean AuthenticationManager authenticationManager;

    @Test
    @DisplayName("sem as telas: nem a fila nem os remetentes")
    @WithMockUser(authorities = "settings/admin:CONFIGURAR")
    void semTela() throws Exception {
        mockMvc.perform(get("/api/dev/email-queue")).andExpect(status().isForbidden());
        mockMvc.perform(get("/api/dev/email-senders")).andExpect(status().isForbidden());
        mockMvc.perform(get("/api/dev/email-queue/insights")).andExpect(status().isForbidden());
        verifyNoInteractions(queue, senders, insights);
    }

    @Test
    @DisplayName("a análise abre com CONSULTAR da fila, e o período chega ao serviço")
    @WithMockUser(authorities = "dev/email-queue:CONSULTAR")
    void analise() throws Exception {
        mockMvc.perform(get("/api/dev/email-queue/insights").param("since", "2026-09-01")).andExpect(status().isOk());
        verify(insights).insights(null, java.time.LocalDate.of(2026, 9, 1));
    }

    @Test
    @DisplayName("só consultar: vê, mas não reenvia, não cria remetente, não muda rota")
    @WithMockUser(authorities = {"dev/email-queue:CONSULTAR", "dev/email-senders:CONSULTAR"})
    void soConsulta() throws Exception {
        mockMvc.perform(post("/api/dev/email-queue/" + UUID.randomUUID() + "/resend").with(csrf())).andExpect(status().isForbidden());
        mockMvc.perform(post("/api/dev/email-senders").with(csrf()).contentType(MediaType.APPLICATION_JSON)
                .content("{\"name\":\"x\",\"displayName\":\"X\"}")).andExpect(status().isForbidden());
        mockMvc.perform(put("/api/dev/email-senders/routes/NEWSLETTER").with(csrf()).contentType(MediaType.APPLICATION_JSON)
                .content("{}")).andExpect(status().isForbidden());
        verifyNoInteractions(queue, senders);
    }

    @Test
    @DisplayName("com ALTERAR: reenvia, e a rota leva o login de quem mudou")
    @WithMockUser(username = "murillo", authorities = {"dev/email-queue:ALTERAR", "dev/email-senders:ALTERAR"})
    void comAlterar() throws Exception {
        UUID id = UUID.randomUUID();
        mockMvc.perform(post("/api/dev/email-queue/" + id + "/resend").with(csrf())).andExpect(status().isOk());
        verify(queue).resend(id);
        mockMvc.perform(put("/api/dev/email-senders/routes/NEWSLETTER").with(csrf()).contentType(MediaType.APPLICATION_JSON)
                .content("{\"senderId\":null,\"replyToId\":null}")).andExpect(status().isOk());
        verify(senders).updateRoute(eq(EmailOrigin.NEWSLETTER), any(), eq("murillo"));
    }
}
