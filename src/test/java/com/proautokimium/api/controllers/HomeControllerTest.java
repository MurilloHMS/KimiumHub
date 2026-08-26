package com.proautokimium.api.controllers;

import com.proautokimium.api.Infrastructure.services.permission.PermissionService;
import com.proautokimium.api.Application.DTOs.home.HomeSummaryDTO;
import com.proautokimium.api.Infrastructure.repositories.UserRepository;
import com.proautokimium.api.Infrastructure.security.SecurityConfiguration;
import com.proautokimium.api.Infrastructure.security.TokenService;
import com.proautokimium.api.Infrastructure.services.home.HomeSummaryService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(HomeController.class)
@TestPropertySource(properties = {"server.port=0"})
@Import(SecurityConfiguration.class)
class HomeControllerTest {

    @Autowired MockMvc mockMvc;

    @MockitoBean HomeSummaryService service;
    @MockitoBean TokenService tokenService;
    // O SecurityFilter passa a somar as permissões de tela às roles.
    @MockitoBean PermissionService permissionService;
    @MockitoBean AuthenticationManager authenticationManager;
    @MockitoBean UserRepository userRepository;

    private void resumoVazio() {
        when(service.getSummary(anyString(), anyBoolean()))
                .thenReturn(new HomeSummaryDTO(List.of(), List.of(), null));
    }

    @Test
    @DisplayName("GET /api/home/summary - sem autenticação devolve 403")
    void semAutenticacaoDeveRecusar() throws Exception {
        mockMvc.perform(get("/api/home/summary"))
                .andExpect(status().isForbidden());
    }

    /** Todo funcionário autenticado tem uma home — não há papel mínimo aqui. */
    @Test
    @DisplayName("GET /api/home/summary - funcionário sem papel de gestão recebe 200")
    void funcionarioComumRecebeAHome() throws Exception {
        resumoVazio();

        mockMvc.perform(get("/api/home/summary").with(usuario("VENDEDOR")))
                .andExpect(status().isOk());
    }

    /**
     * **O que este teste protege é o `false`.**
     *
     * O booleano decide se a resposta carrega nome e valor de pedidos de
     * outras pessoas. Se o controller lesse o papel errado, a lista de
     * aprovações desceria para quem não pode vê-la — e esconder no front não
     * resolveria: bastaria abrir a aba Network.
     */
    @Test
    @DisplayName("Papel sem gestão desce como isRh=false")
    void papelComumDesceComoFalse() throws Exception {
        resumoVazio();

        mockMvc.perform(get("/api/home/summary").with(usuario("VENDEDOR")))
                .andExpect(status().isOk());

        verify(service).getSummary(anyString(), eq(false));
    }

    @Test
    @DisplayName("RH desce como isRh=true")
    void rhDesceComoTrue() throws Exception {
        resumoVazio();

        mockMvc.perform(get("/api/home/summary").with(usuario("RH")))
                .andExpect(status().isOk());

        verify(service).getSummary(anyString(), eq(true));
    }

    @Test
    @DisplayName("ADMIN desce como isRh=true")
    void adminDesceComoTrue() throws Exception {
        resumoVazio();

        mockMvc.perform(get("/api/home/summary").with(usuario("ADMIN")))
                .andExpect(status().isOk());

        verify(service).getSummary(anyString(), eq(true));
    }

    /**
     * CLIENTE é negado pelo `anyRequest()` do SecurityConfiguration, não por
     * anotação neste controller. O teste existe para o dia em que alguém
     * afrouxar aquela regra: a home do ERP não é do cliente.
     */
    @Test
    @DisplayName("CLIENTE não acessa a home do ERP")
    void clienteNaoAcessa() throws Exception {
        mockMvc.perform(get("/api/home/summary").with(usuario("CLIENTE")))
                .andExpect(status().isForbidden());
    }

    private static org.springframework.test.web.servlet.request.RequestPostProcessor usuario(String papel) {
        return org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors
                .user("murillo").roles(papel);
    }
}
