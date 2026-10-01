package com.proautokimium.api.controllers.events;

import com.proautokimium.api.Infrastructure.repositories.UserRepository;
import com.proautokimium.api.Infrastructure.security.SecurityConfiguration;
import com.proautokimium.api.Infrastructure.security.TokenService;
import com.proautokimium.api.Infrastructure.services.events.EventAttendanceService;
import com.proautokimium.api.Infrastructure.services.permission.PermissionService;
import com.proautokimium.api.domain.enums.events.EventAnswer;
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
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Ser convidado basta para responder — sem permissão de tela nenhuma. O
 * Acompanhamento, que mostra a resposta dos outros, é do cadastro.
 */
@WebMvcTest(EventAttendanceController.class)
@TestPropertySource(properties = {"server.port=0"})
@Import(SecurityConfiguration.class)
class EventAttendancePermissionTest {

    @Autowired MockMvc mockMvc;

    @MockitoBean EventAttendanceService service;
    @MockitoBean PermissionService permissionService;
    @MockitoBean UserRepository userRepository;
    @MockitoBean TokenService tokenService;
    @MockitoBean AuthenticationManager authenticationManager;

    @Test
    @DisplayName("funcionário sem tela nenhuma vê os próprios convites")
    @WithMockUser(username = "diego")
    void invitationsWithoutScreen() throws Exception {
        mockMvc.perform(get("/api/events/invitations")).andExpect(status().isOk());

        verify(service).myInvitations("diego");
    }

    @Test
    @DisplayName("responde com o próprio login, nunca com um do corpo do pedido")
    @WithMockUser(username = "diego")
    void respondsAsHimself() throws Exception {
        UUID id = UUID.randomUUID();

        mockMvc.perform(post("/api/events/{id}/response", id).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"answer\":\"GOING\",\"note\":\"ok\",\"login\":\"carlos\"}"))
                .andExpect(status().isOk());

        verify(service).respond(id, "diego", EventAnswer.GOING, "ok");
    }

    @Test
    @DisplayName("observação com 501 caracteres para no @Valid, com 400")
    @WithMockUser(username = "diego")
    void noteTooLong() throws Exception {
        mockMvc.perform(post("/api/events/{id}/response", UUID.randomUUID()).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"answer\":\"GOING\",\"note\":\"" + "a".repeat(501) + "\"}"))
                .andExpect(status().isBadRequest());

        verify(service, never()).respond(any(), anyString(), any(), any());
    }

    @Test
    @DisplayName("sem login, nada")
    void anonymous() throws Exception {
        mockMvc.perform(get("/api/events/invitations")).andExpect(status().isForbidden());

        verifyNoInteractions(service);
    }

    @Test
    @DisplayName("o cliente da Área do Cliente não tem convite")
    @WithMockUser(username = "cliente", roles = "CLIENTE")
    void customerPortal() throws Exception {
        mockMvc.perform(get("/api/events/invitations")).andExpect(status().isForbidden());

        verifyNoInteractions(service);
    }

    @Test
    @DisplayName("quem só vê Documentos não abre o Acompanhamento")
    @WithMockUser(authorities = {"documentos/eventos:CONSULTAR"})
    void attendanceIsForManagers() throws Exception {
        mockMvc.perform(get("/api/events/{id}/attendance", UUID.randomUUID())).andExpect(status().isForbidden());

        verifyNoInteractions(service);
    }

    @Test
    @DisplayName("quem consulta o cadastro abre o Acompanhamento")
    @WithMockUser(authorities = {"communication/events:CONSULTAR"})
    void managerSeesAttendance() throws Exception {
        UUID id = UUID.randomUUID();

        mockMvc.perform(get("/api/events/{id}/attendance", id)).andExpect(status().isOk());

        verify(service).attendance(eq(id));
    }

    @Test
    @DisplayName("as opções do público são de quem cria ou edita, não de quem só consulta")
    @WithMockUser(authorities = {"communication/events:CONSULTAR"})
    void audienceOptionsNeedEdit() throws Exception {
        mockMvc.perform(get("/api/events/audience-options")).andExpect(status().isForbidden());

        verifyNoInteractions(service);
    }
}
