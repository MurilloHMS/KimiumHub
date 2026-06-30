package com.proautokimium.api.Infrastructure.services.product;

import com.proautokimium.api.Application.DTOs.product.ProductWebSiteCreateDTO;
import com.proautokimium.api.Application.DTOs.product.ProductWebSitePublicResponseDTO;
import com.proautokimium.api.Application.DTOs.product.ProductWebSiteResponseDTO;
import com.proautokimium.api.Application.DTOs.product.ProductWebSiteUpdateDTO;
import com.proautokimium.api.Infrastructure.converters.ProductWebSiteConverter;
import com.proautokimium.api.Infrastructure.exceptions.product.ProductNotFoundException;
import com.proautokimium.api.Infrastructure.repositories.ProductWebSiteRepository;
import com.proautokimium.api.Infrastructure.services.storage.ProductImageStorageService;
import com.proautokimium.api.domain.entities.ProductWebsite;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;

import java.io.IOException;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ProductWebsiteServiceTest {

    @Mock
    private ProductWebSiteRepository repository;

    @Mock
    private ProductWebSiteConverter converter;

    @Mock
    private ProductImageStorageService storage;

    @InjectMocks
    private ProductWebsiteService productWebsiteService;

    private UUID productId;
    private ProductWebsite entity;
    private ProductWebSiteCreateDTO createDto;
    private ProductWebSiteUpdateDTO updateDto;

    @BeforeEach
    void setUp() {
        productId = UUID.randomUUID();
        entity = mock(ProductWebsite.class);
        createDto = new ProductWebSiteCreateDTO("SYS001", "Produto Teste", true, List.of("Azul"), "Limpeza", "1:10", "100%", "Cozinha", "Descrição", null);
        updateDto = new ProductWebSiteUpdateDTO("Produto Atualizado", true, List.of("Verde"), "Industrial", "1:20", "50%", "Geral", "Nova descrição", null);
    }

    @Test
    @DisplayName("Deve criar produto sem imagem com sucesso")
    void deveCriarProdutoSemImagemComSucesso() throws IOException {
        when(converter.fromCreateDto(createDto)).thenReturn(entity);
        when(repository.save(entity)).thenReturn(entity);

        assertThatCode(() -> productWebsiteService.create(createDto, null)).doesNotThrowAnyException();
        verify(repository).save(entity);
        verify(storage, never()).save(any(), any());
    }

    @Test
    @DisplayName("Deve criar produto com imagem com sucesso")
    void deveCriarProdutoComImagemComSucesso() throws IOException {
        MockMultipartFile imagem = new MockMultipartFile("imagem", "produto.jpg", "image/jpeg", "dados".getBytes());
        when(converter.fromCreateDto(createDto)).thenReturn(entity);
        when(storage.save(imagem, "SYS001")).thenReturn("produto.jpg");
        when(repository.save(entity)).thenReturn(entity);

        assertThatCode(() -> productWebsiteService.create(createDto, imagem)).doesNotThrowAnyException();
        verify(storage).save(imagem, "SYS001");
        verify(entity).setImagem("produto.jpg");
        verify(repository).save(entity);
    }

    @Test
    @DisplayName("Deve atualizar produto com sucesso")
    void deveAtualizarProdutoComSucesso() throws IOException {
        when(repository.findById(productId)).thenReturn(Optional.of(entity));
        when(repository.save(entity)).thenReturn(entity);

        assertThatCode(() -> productWebsiteService.update(updateDto, productId, null)).doesNotThrowAnyException();
        verify(converter).updateFromDto(updateDto, entity);
        verify(repository).save(entity);
    }

    @Test
    @DisplayName("Deve lançar ProductNotFoundException ao atualizar produto inexistente")
    void deveLancarExcecaoAoAtualizarProdutoInexistente() {
        when(repository.findById(productId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> productWebsiteService.update(updateDto, productId, null))
                .isInstanceOf(ProductNotFoundException.class);

        verify(repository, never()).save(any());
    }

    @Test
    @DisplayName("Deve deletar produto quando existe")
    void deveDeletarProdutoQuandoExiste() {
        when(repository.existsById(productId)).thenReturn(true);

        assertThatCode(() -> productWebsiteService.delete(productId)).doesNotThrowAnyException();
        verify(repository).deleteById(productId);
    }

    @Test
    @DisplayName("Não deve deletar produto quando não existe")
    void naoDeveDeletarProdutoQuandoNaoExiste() {
        when(repository.existsById(productId)).thenReturn(false);

        assertThatCode(() -> productWebsiteService.delete(productId)).doesNotThrowAnyException();
        verify(repository, never()).deleteById(any());
    }

    @Test
    @DisplayName("Deve ocultar produto com sucesso")
    void deveOcultarProdutoComSucesso() {
        when(repository.findById(productId)).thenReturn(Optional.of(entity));
        when(repository.save(entity)).thenReturn(entity);

        assertThatCode(() -> productWebsiteService.hide(productId)).doesNotThrowAnyException();
        verify(entity).setActive(false);
        verify(repository).save(entity);
    }

    @Test
    @DisplayName("Deve lançar ProductNotFoundException ao ocultar produto inexistente")
    void deveLancarExcecaoAoOcultarProdutoInexistente() {
        when(repository.findById(productId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> productWebsiteService.hide(productId))
                .isInstanceOf(ProductNotFoundException.class);
    }

    @Test
    @DisplayName("Deve tornar produto visível com sucesso")
    void deveTornarProdutoVisivelComSucesso() {
        when(repository.findById(productId)).thenReturn(Optional.of(entity));
        when(repository.save(entity)).thenReturn(entity);

        assertThatCode(() -> productWebsiteService.unhide(productId)).doesNotThrowAnyException();
        verify(entity).setActive(true);
        verify(repository).save(entity);
    }

    @Test
    @DisplayName("Deve retornar lista de todos os produtos")
    void deveRetornarTodosOsProdutos() {
        ProductWebSiteResponseDTO responseDto = new ProductWebSiteResponseDTO(productId, "SYS001", "Produto", true, List.of(), null, null, null, null, null, null, null);
        when(repository.findAll()).thenReturn(List.of(entity));
        when(converter.toDto(entity)).thenReturn(responseDto);

        List<ProductWebSiteResponseDTO> result = productWebsiteService.getAll();

        assertThat(result).hasSize(1);
    }

    @Test
    @DisplayName("Deve retornar lista de produtos ativos")
    void deveRetornarProdutosAtivos() {
        ProductWebSitePublicResponseDTO publicDto = mock(ProductWebSitePublicResponseDTO.class);
        when(repository.findAllByActive(true)).thenReturn(List.of(entity));
        when(converter.toPublicDto(entity)).thenReturn(publicDto);

        List<ProductWebSitePublicResponseDTO> result = productWebsiteService.getAllactiveProducts();

        assertThat(result).hasSize(1);
    }
}
