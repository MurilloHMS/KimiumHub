package com.proautokimium.api.controllers;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.proautokimium.api.Application.DTOs.profile.ProfileCreateDto;
import com.proautokimium.api.Application.DTOs.profile.ProfileResponseDto;
import com.proautokimium.api.Infrastructure.converters.ProfileConverter;
import com.proautokimium.api.Infrastructure.repositories.UserRepository;
import com.proautokimium.api.Infrastructure.security.SecurityConfiguration;
import com.proautokimium.api.Infrastructure.security.TokenService;
import com.proautokimium.api.Infrastructure.services.permission.PermissionService;
import com.proautokimium.api.Infrastructure.services.vcard.ProfileService;
import com.proautokimium.api.Infrastructure.services.vcard.VCardService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
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
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * **A tela não pode mentir.**
 *
 * A V88 fez a tela de perfil mostrar o botão do cartão por `perfil:INCLUIR`.
 * Os endpoints de escrita ficaram em `hasRole('VENDEDOR')` mais um dia — e
 * enquanto ficaram, quem tinha a permissão sem a role via o botão e levava 403
 * ao clicar. Nada quebra nesse estado: as duas camadas funcionam, e só
 * discordam.
 *
 * É o risco que o plano registrou com estas palavras: botão escondido pelo
 * `*pkCan` e endpoint anotado têm que concordar, senão a divergência vira 403
 * na cara de quem clicou.
 */
@WebMvcTest(VCardController.class)
@TestPropertySource(properties = {"server.port=0", "app.base-url=http://localhost"})
@Import(SecurityConfiguration.class)
class VCardPermissionTest {

    @Autowired MockMvc mockMvc;
    @Autowired ObjectMapper json;

    @MockitoBean VCardService vCardService;
    @MockitoBean ProfileService profileService;
    @MockitoBean ProfileConverter converter;
    @MockitoBean TokenService tokenService;
    @MockitoBean PermissionService permissionService;
    @MockitoBean AuthenticationManager authenticationManager;
    @MockitoBean UserRepository userRepository;

    private static ProfileCreateDto corpo() {
        return new ProfileCreateDto("Ricardo Souza", "ricardo-souza", "Consultor",
                "Proauto Kimium", "ricardo@proautokimium.com.br", null, null,
                List.of(), List.of(), List.of(), List.of(), true);
    }

    private ProfileResponseDto resposta() {
        return new ProfileResponseDto(UUID.randomUUID(), "Ricardo Souza", "ricardo-souza",
                "Consultor", "Proauto Kimium", "ricardo@proautokimium.com.br",
                null, null, List.of(), List.of(), List.of(), List.of(), true);
    }

    /**
     * **O teste que fecha a divergência.**
     *
     * A permissão sozinha basta — sem role nenhuma de vendas.
     */
    @Test
    @DisplayName("com perfil:INCLUIR, cria o cartão sem precisar da role")
    @WithMockUser(username = "ricardo", authorities = {"ROLE_USER", "perfil:INCLUIR"})
    void permissaoBasta() throws Exception {
        when(profileService.createMyProfile(anyString(), any())).thenReturn(resposta());

        mockMvc.perform(post("/api/profile/me")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json.writeValueAsString(corpo())))
                .andExpect(status().is2xxSuccessful());
    }

    /**
     * **E a role sozinha não basta mais.**
     *
     * Se este teste falhar, a regra velha voltou — e o combinado de configurar
     * pela tela em vez de por código foi desfeito sem ninguém dizer.
     */
    @Test
    @DisplayName("só a role VENDEDOR não cria mais nada")
    @WithMockUser(username = "ricardo", authorities = {"ROLE_VENDEDOR"})
    void roleNaoBastaMais() throws Exception {
        mockMvc.perform(post("/api/profile/me")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json.writeValueAsString(corpo())))
                .andExpect(status().isForbidden());

        verifyNoInteractions(profileService);
    }

    /**
     * Editar tem permissão própria, e a V89 a deu a quem pode criar.
     *
     * Deixar `ALTERAR` ligado no modelo Base — como a V88 deixou por um dia —
     * abriria a edição para a empresa inteira. Na prática ninguém sem cartão
     * consegue editar, mas "na prática ninguém consegue" é uma defesa que
     * depende de outra camada não mudar.
     */
    @Test
    @DisplayName("editar o cartão exige perfil:ALTERAR, e INCLUIR sozinho não serve")
    @WithMockUser(username = "ricardo", authorities = {"perfil:INCLUIR"})
    void editarExigeAlterar() throws Exception {
        mockMvc.perform(put("/api/profile/me")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json.writeValueAsString(corpo())))
                .andExpect(status().isForbidden());
    }

    /**
     * A foto é parte do cartão, e segue a permissão de editá-lo.
     *
     * Com arquivo de verdade no corpo: o `@PreAuthorize` roda **depois** da
     * resolução dos argumentos, então uma requisição sem o `file` estoura em
     * 500 antes de chegar à autorização — e o teste estaria medindo outra
     * coisa.
     */
    @Test
    @DisplayName("a foto do cartão segue perfil:ALTERAR")
    @WithMockUser(username = "ricardo", authorities = {"ROLE_USER"})
    void fotoSegueAlterar() throws Exception {
        MockMultipartFile arquivo = new MockMultipartFile(
                "file", "foto.png", MediaType.IMAGE_PNG_VALUE, new byte[]{1, 2, 3});

        mockMvc.perform(multipart("/api/profile/me/image").file(arquivo).with(csrf()))
                .andExpect(status().isForbidden());
    }
}
