package com.proautokimium.api.controllers;

import com.proautokimium.api.Infrastructure.repositories.SmtpEmailRepository;
import com.proautokimium.api.Infrastructure.repositories.UserRepository;
import com.proautokimium.api.Infrastructure.security.SecurityConfiguration;
import com.proautokimium.api.Infrastructure.security.TokenService;
import com.proautokimium.api.Infrastructure.services.email.EmailQueueService;
import com.proautokimium.api.Infrastructure.services.permission.PermissionService;
import com.proautokimium.api.domain.entities.EmailEntity;
import com.proautokimium.api.domain.valueObjects.Email;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * O envio manual da tela de Comunicação passa pela fila: um e-mail por
 * destinatário, com o remetente escolhido entre os e-mails da empresa.
 */
@WebMvcTest(SmtpController.class)
@TestPropertySource(properties = {"server.port=0"})
@Import(SecurityConfiguration.class)
class SmtpControllerTest {

    @Autowired MockMvc mockMvc;
    @MockitoBean EmailQueueService emailQueue;
    @MockitoBean SmtpEmailRepository senders;
    @MockitoBean TokenService tokenService;
    @MockitoBean PermissionService permissionService;
    @MockitoBean AuthenticationManager authenticationManager;
    @MockitoBean UserRepository userRepository;

    private static EmailEntity remetente(String address, boolean active) {
        EmailEntity e = new EmailEntity();
        e.setName(address.split("@")[0]);
        e.setEmail(new Email(address));
        e.setDisplayName("Comercial Proauto");
        e.setActive(active);
        return e;
    }

    private static MockMultipartFile data(String json) {
        return new MockMultipartFile("data", "", MediaType.APPLICATION_JSON_VALUE, json.getBytes());
    }

    @Test
    @DisplayName("enfileira um e-mail por destinatário, com o remetente escolhido e o anexo")
    @WithMockUser(authorities = "settings/admin:CONFIGURAR")
    void enfileiraPorDestinatario() throws Exception {
        when(senders.findAll()).thenReturn(List.of(remetente("comercial@envios.proautokimium.com.br", true)));

        mockMvc.perform(multipart("/api/smtp/send")
                        .file(data("{\"recipients\":[\"a@x.com\",\"b@x.com\"],\"sender\":\"comercial@envios.proautokimium.com.br\","
                                + "\"subject\":\"Oi\",\"body\":\"<p>Oi</p>\"}"))
                        .file(new MockMultipartFile("attachments", "tabela.pdf", "application/pdf", new byte[]{1, 2}))
                        .with(csrf()))
                .andExpect(status().isAccepted())
                .andExpect(content().string("2 e-mail(s) na fila. Saem no próximo minuto."));

        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<EmailQueueService.OutgoingAttachment>> anexos = ArgumentCaptor.forClass(List.class);
        verify(emailQueue).enqueueAs(eq("comercial@envios.proautokimium.com.br"), eq("Comercial Proauto"), isNull(),
                eq("a@x.com"), eq("Oi"), eq("<p>Oi</p>"), anexos.capture());
        verify(emailQueue).enqueueAs(anyString(), anyString(), any(), eq("b@x.com"), anyString(), anyString(), anyList());
        assertThat(anexos.getValue()).singleElement().satisfies(a -> assertThat(a.filename()).isEqualTo("tabela.pdf"));
    }

    @Test
    @DisplayName("remetente que não é e-mail da empresa, ou está inativo: 400, nada entra na fila")
    @WithMockUser(authorities = "settings/admin:CONFIGURAR")
    void remetenteInvalido() throws Exception {
        when(senders.findAll()).thenReturn(List.of(remetente("velho@envios.proautokimium.com.br", false)));

        mockMvc.perform(multipart("/api/smtp/send")
                        .file(data("{\"recipients\":[\"a@x.com\"],\"sender\":\"velho@envios.proautokimium.com.br\",\"subject\":\"x\",\"body\":\"x\"}"))
                        .with(csrf()))
                .andExpect(status().isBadRequest());
        mockMvc.perform(multipart("/api/smtp/send")
                        .file(data("{\"recipients\":[\"a@x.com\"],\"sender\":\"qualquer@gmail.com\",\"subject\":\"x\",\"body\":\"x\"}"))
                        .with(csrf()))
                .andExpect(status().isBadRequest());
        verifyNoInteractions(emailQueue);
    }

    @Test
    @DisplayName("sem a permissão: 403")
    @WithMockUser(authorities = "communication/email:CONSULTAR")
    void semPermissao() throws Exception {
        mockMvc.perform(multipart("/api/smtp/send").file(data("{\"recipients\":[\"a@x.com\"]}")).with(csrf()))
                .andExpect(status().isForbidden());
        verifyNoInteractions(emailQueue);
    }
}
