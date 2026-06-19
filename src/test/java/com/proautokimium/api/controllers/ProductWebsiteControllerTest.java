package com.proautokimium.api.controllers;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.proautokimium.api.Application.DTOs.product.ProductWebSiteCreateDTO;
import com.proautokimium.api.Application.DTOs.product.ProductWebSitePublicResponseDTO;
import com.proautokimium.api.Application.DTOs.product.ProductWebSiteResponseDTO;
import com.proautokimium.api.Application.DTOs.product.ProductWebSiteUpdateDTO;
import com.proautokimium.api.Infrastructure.exceptions.product.ProductNotFoundException;
import com.proautokimium.api.Infrastructure.repositories.UserRepository;
import com.proautokimium.api.Infrastructure.security.SecurityConfiguration;
import com.proautokimium.api.Infrastructure.security.TokenService;
import com.proautokimium.api.Infrastructure.services.product.ProductWebsiteService;
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
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(ProductWebsiteController.class)
@TestPropertySource(properties = {"server.port=0"})
@Import(SecurityConfiguration.class)
class ProductWebsiteControllerTest {

    @Autowired MockMvc mockMvc;
    @Autowired ObjectMapper objectMapper;
    @MockitoBean ProductWebsiteService service;
    @MockitoBean TokenService tokenService;
    @MockitoBean AuthenticationManager authenticationManager;
    @MockitoBean UserRepository userRepository;

    private final UUID productId = UUID.randomUUID();

    @Test
    @DisplayName("GET /api/product/website/active - deve retornar produtos ativos sem autenticação")
    void deveRetornarProdutosAtivos() throws Exception {
        when(service.getAllactiveProducts()).thenReturn(List.of(mock(ProductWebSitePublicResponseDTO.class)));

        mockMvc.perform(get("/api/product/website/active"))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("GET /api/product/website - deve retornar lista de produtos quando autenticado")
    @WithMockUser
    void deveRetornarTodosOsProdutosAutenticado() throws Exception {
        when(service.getAll()).thenReturn(List.of());

        mockMvc.perform(get("/api/product/website"))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("GET /api/product/website - deve retornar 403 sem autenticação")
    void deveRetornar403SemAutenticacao() throws Exception {
        mockMvc.perform(get("/api/product/website"))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("POST /api/product/website - deve criar produto e retornar 201 quando autenticado")
    @WithMockUser
    void deveCriarProdutoComSucesso() throws Exception {
        doNothing().when(service).create(any(), any());

        MockMultipartFile dados = new MockMultipartFile(
                "dados", "", MediaType.APPLICATION_JSON_VALUE,
                objectMapper.writeValueAsBytes(new ProductWebSiteCreateDTO("SYS001", "Produto", true, List.of(), "Limpeza", "1:10", "100%", "Cozinha", "Descrição")));

        mockMvc.perform(multipart("/api/product/website")
                        .file(dados)
                        .with(csrf()))
                .andExpect(status().isCreated())
                .andExpect(content().string("Produto cadastrado com sucesso"));
    }

    @Test
    @DisplayName("POST /api/product/website - deve retornar 403 sem autenticação")
    void deveRetornar403AoCriarSemAutenticacao() throws Exception {
        MockMultipartFile dados = new MockMultipartFile(
                "dados", "", MediaType.APPLICATION_JSON_VALUE,
                objectMapper.writeValueAsBytes(new ProductWebSiteCreateDTO("SYS001", "Produto", true, List.of(), "Limpeza", "1:10", "100%", "Cozinha", "Descrição")));

        mockMvc.perform(multipart("/api/product/website")
                        .file(dados)
                        .with(csrf()))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("PUT /api/product/website/{id}/hide - deve ocultar produto quando autenticado")
    @WithMockUser
    void deveOcultarProdutoComSucesso() throws Exception {
        doNothing().when(service).hide(productId);

        mockMvc.perform(put("/api/product/website/{id}/hide", productId)
                        .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(content().string("Produto ocultado com sucesso"));
    }

    @Test
    @DisplayName("PUT /api/product/website/{id}/unhide - deve tornar produto visível quando autenticado")
    @WithMockUser
    void deveTornarProdutoVisivelComSucesso() throws Exception {
        doNothing().when(service).unhide(productId);

        mockMvc.perform(put("/api/product/website/{id}/unhide", productId)
                        .with(csrf()))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("PUT /api/product/website/{id}/hide - deve retornar 404 quando produto não existe")
    @WithMockUser
    void deveRetornar404AoOcultarProdutoInexistente() throws Exception {
        doThrow(new ProductNotFoundException()).when(service).hide(productId);

        mockMvc.perform(put("/api/product/website/{id}/hide", productId)
                        .with(csrf()))
                .andExpect(status().isNotFound());
    }
}
