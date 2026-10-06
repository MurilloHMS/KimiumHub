package com.proautokimium.api.controllers.humanResources;

import com.proautokimium.api.Infrastructure.repositories.UserRepository;
import com.proautokimium.api.Infrastructure.security.SecurityConfiguration;
import com.proautokimium.api.Infrastructure.security.TokenService;
import com.proautokimium.api.Infrastructure.services.events.EventAttendanceService;
import com.proautokimium.api.Infrastructure.services.humanResources.DocumentRequestService;
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

import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
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
 * As duas telas das Solicitações, e o que cada uma deixa fazer. O portal do
 * funcionário não confere nem envia; a tela do RH não responde por ninguém.
 */
@WebMvcTest(DocumentRequestController.class)
@TestPropertySource(properties = {"server.port=0"})
@Import(SecurityConfiguration.class)
class DocumentRequestPermissionTest {

    @Autowired MockMvc mockMvc;

    @MockitoBean DocumentRequestService service;
    @MockitoBean EventAttendanceService audience;
    @MockitoBean PermissionService permissionService;
    @MockitoBean UserRepository userRepository;
    @MockitoBean TokenService tokenService;
    @MockitoBean AuthenticationManager authenticationManager;

    private static final String BASE = "/api/hr/document-requests";

    @Test
    @DisplayName("o portal do funcionário não lista, não aprova e não envia solicitação")
    @WithMockUser(authorities = {"documentos/rh/requests:CONSULTAR", "documentos/rh/requests:ALTERAR",
            "documentos/rh/requests:ENVIAR"})
    void portalNaoFazPapelDoRh() throws Exception {
        mockMvc.perform(get(BASE)).andExpect(status().isForbidden());
        mockMvc.perform(post(BASE + "/recipients/" + UUID.randomUUID() + "/approve").with(csrf()))
                .andExpect(status().isForbidden());
        mockMvc.perform(post(BASE + "/" + UUID.randomUUID() + "/send").with(csrf())
                .contentType(MediaType.APPLICATION_JSON).content("{\"all\":true}"))
                .andExpect(status().isForbidden());
        verifyNoInteractions(service);
    }

    @Test
    @DisplayName("quem só consulta no RH não aprova, não devolve e não envia")
    @WithMockUser(authorities = {"rh/document-requests:CONSULTAR"})
    void consultarNaoConfere() throws Exception {
        mockMvc.perform(post(BASE + "/recipients/" + UUID.randomUUID() + "/approve").with(csrf()))
                .andExpect(status().isForbidden());
        mockMvc.perform(post(BASE + "/recipients/" + UUID.randomUUID() + "/return").with(csrf())
                .contentType(MediaType.APPLICATION_JSON).content("{\"reason\":\"x\"}"))
                .andExpect(status().isForbidden());
        mockMvc.perform(post(BASE + "/" + UUID.randomUUID() + "/send").with(csrf())
                .contentType(MediaType.APPLICATION_JSON).content("{\"all\":true}"))
                .andExpect(status().isForbidden());
        verifyNoInteractions(service);
    }

    @Test
    @DisplayName("a tela do RH não responde por ninguém: responder e anexar são do portal")
    @WithMockUser(authorities = {"rh/document-requests:ALTERAR", "rh/document-requests:INCLUIR"})
    void rhNaoResponde() throws Exception {
        UUID recipientId = UUID.randomUUID();
        mockMvc.perform(post(BASE + "/me/" + recipientId + "/submit").with(csrf())
                .contentType(MediaType.APPLICATION_JSON).content("{\"answers\":{}}"))
                .andExpect(status().isForbidden());
        mockMvc.perform(multipart(BASE + "/me/" + recipientId + "/files")
                        .file("file", "%PDF".getBytes()).param("fieldKey", "rg").with(csrf()))
                .andExpect(status().isForbidden());
        verifyNoInteractions(service);
    }

    @Test
    @DisplayName("responder leva o login de quem está logado, nunca um id de funcionário vindo de fora")
    @WithMockUser(username = "ana", authorities = {"documentos/rh/requests:INCLUIR"})
    void responderUsaOLogin() throws Exception {
        UUID recipientId = UUID.randomUUID();
        mockMvc.perform(post(BASE + "/me/" + recipientId + "/submit").with(csrf())
                        .contentType(MediaType.APPLICATION_JSON).content("{\"answers\":{\"camisa\":\"M\"}}"))
                .andExpect(status().isOk());
        verify(service).submit(eq(recipientId), eq("ana"), any());
    }

    @Test
    @DisplayName("aprovar com ALTERAR no RH leva o login de quem confere")
    @WithMockUser(username = "rita", authorities = {"rh/document-requests:ALTERAR"})
    void aprovarLevaOLogin() throws Exception {
        UUID recipientId = UUID.randomUUID();
        mockMvc.perform(post(BASE + "/recipients/" + recipientId + "/approve").with(csrf()))
                .andExpect(status().isOk());
        verify(service).approve(recipientId, "rita");
    }

    @Test
    @DisplayName("baixar: só o RH com BAIXAR vê de todos; o portal baixa como dono")
    @WithMockUser(username = "ana", authorities = {"documentos/rh/requests:BAIXAR"})
    void baixarComoDono() throws Exception {
        UUID fileId = UUID.randomUUID();
        when(service.readFile(any(), any(), anyBoolean()))
                .thenReturn(new DocumentRequestService.FileContent("rg.pdf", "application/pdf", "%PDF".getBytes()));

        mockMvc.perform(get(BASE + "/files/" + fileId)).andExpect(status().isOk());

        verify(service).readFile(fileId, "ana", false);
    }

    @Test
    @DisplayName("sem nenhuma das duas telas, não baixa")
    @WithMockUser(authorities = {"rh/reimbursements:BAIXAR"})
    void baixarSemTela() throws Exception {
        mockMvc.perform(get(BASE + "/files/" + UUID.randomUUID())).andExpect(status().isForbidden());
        verifyNoInteractions(service);
    }
}
