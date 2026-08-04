package com.proautokimium.api.Infrastructure.services.gallery;

import com.proautokimium.api.Application.DTOs.gallery.CreateGalleryDocumentDTO;
import com.proautokimium.api.Infrastructure.repositories.gallery.GalleryDocumentRepository;
import com.proautokimium.api.Infrastructure.services.authentication.AuthorizationService;
import com.proautokimium.api.Infrastructure.services.storage.GalleryStorageService;
import com.proautokimium.api.domain.entities.auth.User;
import com.proautokimium.api.domain.entities.gallery.GalleryDocument;
import com.proautokimium.api.domain.enums.UserRole;
import com.proautokimium.api.domain.enums.gallery.Category;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class GalleryDocumentServiceTest {

    @Mock private GalleryDocumentRepository repository;
    @Mock private GalleryStorageService storageService;
    @Mock private AuthorizationService authorizationService;

    private GalleryDocumentService service;
    private Clock clock;

    private UUID documentId;
    private GalleryDocument entity;
    private CreateGalleryDocumentDTO createDto;
    private User user;
    private MultipartFile imagem;

    @BeforeEach
    void setup(){
        clock = Clock.fixed(Instant.now(), ZoneId.systemDefault());
        service = new GalleryDocumentService(storageService, repository, authorizationService, clock);
        documentId = UUID.randomUUID();
        entity = mock(GalleryDocument.class);
        createDto = new CreateGalleryDocumentDTO("Logo empresa", "Logo da empresa", Category.LOGO);
        user = new User("usuario.padrao", "user@teste.com", "hash", List.of(UserRole.ADMIN));
        imagem = new MockMultipartFile("logo", "logo.png", "image/png", "data".getBytes());
    }

    @Test
    @DisplayName("Deve criar um documento")
    void shouldCreateDocument() throws IOException {
        when(authorizationService.loadUserByUsername("usuario.padrao")).thenReturn(user);
        when(storageService.save(imagem, "gallery")).thenReturn("/upload/gallery/gallery-uuid.png");
        when(repository.save(any(GalleryDocument.class))).thenAnswer(i -> i.getArgument(0));

        var result = service.create(imagem, createDto, "usuario.padrao");

        assertThat(result).isNotNull();
        assertThat(result.title()).isEqualTo("Logo empresa");
        assertThat(result.category()).isEqualTo(Category.LOGO);
        assertThat(result.originalFilename()).isEqualTo("logo.png");
        verify(storageService).save(imagem, "gallery");
        verify(repository).save(any(GalleryDocument.class));
    }

    @Test
    @DisplayName("Deve encontrar documento pelo ID")
    void shouldFindDocumentById() {
        when(repository.findById(documentId)).thenReturn(Optional.of(entity));

        var result = service.findById(documentId);

        assertThat(result).isEqualTo(entity);
        verify(repository).findById(documentId);
    }

    @Test
    @DisplayName("Deve lançar exceção quando documento não existe")
    void shouldThrowWhenDocumentNotFound() {
        when(repository.findById(documentId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.findById(documentId))
                .isInstanceOf(com.proautokimium.api.Infrastructure.exceptions.file.FileNotFoundException.class);
    }

    @Test
    @DisplayName("Deve deletar documento")
    void shouldDeleteDocument(@TempDir Path tempDir) throws IOException {
        Path tempFile = Files.createFile(tempDir.resolve("gallery-uuid.png"));
        when(repository.findById(documentId)).thenReturn(Optional.of(entity));
        when(entity.getStoragePath()).thenReturn("gallery-uuid.png");
        when(storageService.searchFile("gallery-uuid.png")).thenReturn(tempFile);

        assertThatCode(() -> service.delete(documentId)).doesNotThrowAnyException();
        verify(repository).delete(entity);
        assertThat(tempFile).doesNotExist();
    }
}