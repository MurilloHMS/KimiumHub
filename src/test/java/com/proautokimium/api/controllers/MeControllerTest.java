package com.proautokimium.api.controllers;

import com.proautokimium.api.Infrastructure.repositories.UserRepository;
import com.proautokimium.api.Infrastructure.security.SecurityConfiguration;
import com.proautokimium.api.Infrastructure.security.TokenService;
import com.proautokimium.api.Infrastructure.services.permission.PermissionService;
import com.proautokimium.api.domain.entities.auth.User;
import com.proautokimium.api.domain.enums.UserRole;
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

import java.util.List;
import java.util.Map;

import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * O endpoint que o front consome logo depois do login.
 *
 * Duas coisas se protegem aqui, e nenhuma é o caminho feliz: que ele **exija
 * login** — senão qualquer um descobre o desenho de permissões da empresa — e
 * que ele devolva **um mapa**, porque o guard e a diretiva do front dependem
 * desse formato para não varrer duzentas strings a cada render.
 */
@WebMvcTest(MeController.class)
@TestPropertySource(properties = {"server.port=0"})
@Import(SecurityConfiguration.class)
class MeControllerTest {

    @Autowired MockMvc mockMvc;

    @MockitoBean UserRepository userRepository;
    @MockitoBean PermissionService permissionService;
    @MockitoBean TokenService tokenService;
    @MockitoBean AuthenticationManager authenticationManager;

    private User usuario() {
        User user = new User("murillo", "murillo@teste.com", "Senha123@", List.of(UserRole.USER));
        user.setId("u-1");
        return user;
    }

    /**
     * As permissões da empresa inteira ficariam expostas: quais telas existem,
     * como elas se chamam, e o que se faz nelas. É desenho interno.
     */
    @Test
    @DisplayName("GET api/me/permissions - deve retornar 403 sem autenticação")
    void exigeAutenticacao() throws Exception {
        mockMvc.perform(get("/api/me/permissions"))
                .andExpect(status().isForbidden());

        verifyNoInteractions(permissionService);
    }

    /**
     * O formato é contrato com o front: `{ tela: [acoes] }`.
     *
     * Se virar lista de `tela:ACAO`, o guard e o `*pkCan` param de funcionar —
     * e param em silêncio, escondendo tela de quem tem acesso.
     */
    @Test
    @DisplayName("GET api/me/permissions - devolve as permissões agrupadas por tela")
    @WithMockUser(username = "murillo")
    void devolveAgrupadoPorTela() throws Exception {
        when(userRepository.findByLogin("murillo")).thenReturn(usuario());
        when(permissionService.permissionsByScreen("u-1")).thenReturn(Map.of(
                "stock/movements", List.of("CONSULTAR", "EXCLUIR")));

        mockMvc.perform(get("/api/me/permissions"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$['stock/movements']").isArray())
                .andExpect(jsonPath("$['stock/movements'][0]").value("CONSULTAR"));
    }

    /**
     * Quem ainda não foi configurado recebe `{}`, não erro.
     *
     * O front trata mapa vazio como "não vê nada" e mostra a tela de acesso
     * negado. Um 500 aqui viraria tela branca no login, que é bem pior.
     */
    @Test
    @DisplayName("GET api/me/permissions - sem permissão nenhuma devolve mapa vazio")
    @WithMockUser(username = "murillo")
    void semPermissaoDevolveMapaVazio() throws Exception {
        when(userRepository.findByLogin("murillo")).thenReturn(usuario());
        when(permissionService.permissionsByScreen("u-1")).thenReturn(Map.of());

        mockMvc.perform(get("/api/me/permissions"))
                .andExpect(status().isOk())
                .andExpect(content().json("{}"));
    }
}
