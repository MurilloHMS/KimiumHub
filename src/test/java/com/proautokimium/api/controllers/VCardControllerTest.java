package com.proautokimium.api.controllers;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.proautokimium.api.Application.DTOs.profile.ProfileCreateDto;
import com.proautokimium.api.Application.DTOs.profile.ProfileResponseDto;
import com.proautokimium.api.Infrastructure.converters.ProfileConverter;
import com.proautokimium.api.Infrastructure.repositories.UserRepository;
import com.proautokimium.api.Infrastructure.security.SecurityConfiguration;
import com.proautokimium.api.Infrastructure.security.TokenService;
import com.proautokimium.api.Infrastructure.services.vcard.ProfileService;
import com.proautokimium.api.Infrastructure.services.vcard.VCardService;
import com.proautokimium.api.domain.entities.Profile;
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
import java.util.Optional;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(VCardController.class)
@TestPropertySource(properties = {"server.port=0", "app.base-url=http://localhost"})
@Import(SecurityConfiguration.class)
class VCardControllerTest {

    @Autowired MockMvc mockMvc;
    @Autowired ObjectMapper objectMapper;
    @MockitoBean VCardService vCardService;
    @MockitoBean ProfileService profileService;
    @MockitoBean ProfileConverter converter;
    @MockitoBean TokenService tokenService;
    @MockitoBean AuthenticationManager authenticationManager;
    @MockitoBean UserRepository userRepository;

    private UUID profileId = UUID.randomUUID();

    private ProfileResponseDto buildResponse() {
        return new ProfileResponseDto(profileId, "João Silva", "joao-silva", "Dev", "Empresa",
                "joao@teste.com", null, null, List.of(), List.of(), List.of(), List.of(), true);
    }

    @Test
    @DisplayName("GET /api/profile - deve retornar lista quando autenticado")
    @WithMockUser
    void deveRetornarListaDeProfilesAutenticado() throws Exception {
        when(profileService.getAll()).thenReturn(List.of(buildResponse()));

        mockMvc.perform(get("/api/profile"))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("GET /api/profile - deve retornar 403 sem autenticação")
    void deveRetornar403SemAutenticacao() throws Exception {
        mockMvc.perform(get("/api/profile"))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("POST /api/profile - deve criar profile quando autenticado")
    @WithMockUser
    void deveCriarProfileAutenticado() throws Exception {
        ProfileCreateDto dto = new ProfileCreateDto("João Silva", "joao-silva", "Dev", "Empresa",
                "joao@teste.com", null, null, List.of(), List.of(), List.of(), List.of(), true);
        when(profileService.create(any())).thenReturn(buildResponse());

        mockMvc.perform(post("/api/profile")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("POST /api/profile - deve retornar 403 sem autenticação")
    void deveRetornar403AoCriarSemAutenticacao() throws Exception {
        ProfileCreateDto dto = new ProfileCreateDto("João Silva", "joao-silva", "Dev", "Empresa",
                "joao@teste.com", null, null, List.of(), List.of(), List.of(), List.of(), true);

        mockMvc.perform(post("/api/profile")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("DELETE /api/profile/{id} - deve deletar profile quando autenticado")
    @WithMockUser
    void deveDeletarProfileAutenticado() throws Exception {
        doNothing().when(profileService).delete(profileId);

        mockMvc.perform(delete("/api/profile/{id}", profileId)
                        .with(csrf()))
                .andExpect(status().isNoContent());
    }

    @Test
    @DisplayName("GET /api/profile/public/{slug} - deve retornar profile sem autenticação")
    void deveRetornarProfilePublicoPorSlugSemAutenticacao() throws Exception {
        Profile profile = mock(Profile.class);
        when(profileService.findBySlug("joao-silva")).thenReturn(Optional.of(profile));
        when(converter.toDto(profile)).thenReturn(buildResponse());

        mockMvc.perform(get("/api/profile/public/{slug}", "joao-silva"))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("GET /api/profile/public/{slug}/vcard - deve retornar arquivo vcard sem autenticação")
    void deveRetornarVCardSemAutenticacao() throws Exception {
        Profile profile = mock(Profile.class);
        when(profile.getSlug()).thenReturn("joao-silva");
        when(profileService.findBySlug("joao-silva")).thenReturn(Optional.of(profile));
        when(vCardService.generate(profile)).thenReturn("BEGIN:VCARD".getBytes());

        mockMvc.perform(get("/api/profile/public/{slug}/vcard", "joao-silva"))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Disposition", "attachment; filename=\"joao-silva.vcf\""));
    }

    @Test
    @DisplayName("GET /api/profile/public/{slug}/vcard - deve retornar 404 quando slug não existe")
    void deveRetornar404QuandoSlugNaoExiste() throws Exception {
        when(profileService.findBySlug("slug-inexistente")).thenReturn(Optional.empty());

        mockMvc.perform(get("/api/profile/public/{slug}/vcard", "slug-inexistente"))
                .andExpect(status().isNotFound());
    }
}
