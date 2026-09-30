package com.proautokimium.api.controllers;

import com.proautokimium.api.Application.DTOs.user.LoginResponseDTO;
import com.proautokimium.api.Application.DTOs.webauthn.AuthenticationOptionsDTO;
import com.proautokimium.api.Infrastructure.repositories.UserRepository;
import com.proautokimium.api.Infrastructure.security.SecurityConfiguration;
import com.proautokimium.api.Infrastructure.security.TokenService;
import com.proautokimium.api.Infrastructure.services.authentication.webauthn.WebAuthnService;
import com.proautokimium.api.Infrastructure.services.permission.PermissionService;
import com.proautokimium.api.domain.exceptions.auth.WebAuthnRejectedException;
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
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Quem pode o quê: o login pela digital é público (quem chama ainda não
 * entrou); o resto é da pessoa logada; o cadastro do funcionário segue as
 * permissões da tela {@code rh/employees}.
 */
@WebMvcTest({WebAuthnController.class, EmployeeWebAuthnController.class})
@TestPropertySource(properties = {"server.port=0"})
@Import(SecurityConfiguration.class)
class WebAuthnControllerTest {

    @Autowired MockMvc mockMvc;

    @MockitoBean WebAuthnService service;
    @MockitoBean PermissionService permissionService;
    @MockitoBean UserRepository userRepository;
    @MockitoBean TokenService tokenService;
    @MockitoBean AuthenticationManager authenticationManager;

    static final UUID EMPLOYEE = UUID.fromString("0b0e5a3c-5f1e-4a8d-9d2b-3c1f6a7e8d90");
    static final UUID DEVICE = UUID.fromString("7d7f5a0c-1c2b-4c8e-9a51-2f7b3f7e0a11");

    static final String LOGIN_BODY = """
            {"challengeId":"7d7f5a0c-1c2b-4c8e-9a51-2f7b3f7e0a11","credentialId":"abc","clientDataJSON":"e30",
             "authenticatorData":"AAAA","signature":"AAAA","userHandle":null}
            """;

    @Test
    @DisplayName("o desafio de login sai sem ninguém logado")
    void authenticationOptionsArePublic() throws Exception {
        when(service.authenticationOptions()).thenReturn(new AuthenticationOptionsDTO(DEVICE, "ch", "localhost", 300000));

        mockMvc.perform(post("/api/auth/webauthn/authentication/options").with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.challenge").value("ch"));
    }

    @Test
    @DisplayName("o login pela digital é público e devolve os tokens")
    void authenticationIsPublic() throws Exception {
        when(service.login(any())).thenReturn(new LoginResponseDTO("access", "refresh"));

        mockMvc.perform(post("/api/auth/webauthn/authentication").with(csrf())
                        .contentType(MediaType.APPLICATION_JSON).content(LOGIN_BODY))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").value("access"));
    }

    @Test
    @DisplayName("digital recusada: 401 com a frase da tela")
    void rejectedIs401() throws Exception {
        when(service.login(any())).thenThrow(new WebAuthnRejectedException());

        mockMvc.perform(post("/api/auth/webauthn/authentication").with(csrf())
                        .contentType(MediaType.APPLICATION_JSON).content(LOGIN_BODY))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("ativar a digital exige estar logado")
    void registrationNeedsLogin() throws Exception {
        mockMvc.perform(post("/api/auth/webauthn/registration/options").with(csrf()))
                .andExpect(status().is4xxClientError());
        verifyNoInteractions(service);
    }

    @Test
    @DisplayName("logado, sem permissão de tela nenhuma, ativa e lista os próprios aparelhos")
    @WithMockUser(username = "diego")
    void ownDevicesNeedOnlyLogin() throws Exception {
        when(service.listMine("diego")).thenReturn(List.of());

        mockMvc.perform(post("/api/auth/webauthn/registration/options").with(csrf())).andExpect(status().isOk());
        mockMvc.perform(get("/api/auth/webauthn/credentials")).andExpect(status().isOk());
        mockMvc.perform(delete("/api/auth/webauthn/credentials/" + DEVICE).with(csrf())).andExpect(status().isNoContent());
        verify(service).registrationOptions("diego");
        verify(service).removeMine("diego", DEVICE);
    }

    @Test
    @DisplayName("aparelhos do funcionário: ver é CONSULTAR, remover é ALTERAR")
    @WithMockUser(username = "rh", authorities = "rh/employees:CONSULTAR")
    void employeeDevicesFollowScreenPermissions() throws Exception {
        mockMvc.perform(get("/api/employee/" + EMPLOYEE + "/webauthn-credentials")).andExpect(status().isOk());
        mockMvc.perform(delete("/api/employee/" + EMPLOYEE + "/webauthn-credentials").with(csrf()))
                .andExpect(status().isForbidden());
        verify(service, never()).removeForEmployee(any(), any());
    }

    @Test
    @DisplayName("com ALTERAR, remove um ou todos")
    @WithMockUser(username = "rh", authorities = "rh/employees:ALTERAR")
    void employeeDevicesRemovedWithAlterar() throws Exception {
        mockMvc.perform(delete("/api/employee/" + EMPLOYEE + "/webauthn-credentials/" + DEVICE).with(csrf()))
                .andExpect(status().isNoContent());
        mockMvc.perform(delete("/api/employee/" + EMPLOYEE + "/webauthn-credentials").with(csrf()))
                .andExpect(status().isNoContent());
        verify(service).removeForEmployee(EMPLOYEE, DEVICE);
        verify(service).removeForEmployee(eq(EMPLOYEE), isNull());
    }

    @Test
    @DisplayName("sem permissão na tela de funcionários, nem ver")
    @WithMockUser(username = "diego")
    void employeeDevicesHiddenWithoutPermission() throws Exception {
        mockMvc.perform(get("/api/employee/" + EMPLOYEE + "/webauthn-credentials")).andExpect(status().isForbidden());
    }
}
