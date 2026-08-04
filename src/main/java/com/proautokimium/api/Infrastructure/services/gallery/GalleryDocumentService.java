package com.proautokimium.api.Infrastructure.services.gallery;

import com.proautokimium.api.Application.DTOs.gallery.CreateGalleryDocumentDTO;
import com.proautokimium.api.Application.DTOs.gallery.GalleryDocumentResponseDTO;
import com.proautokimium.api.Infrastructure.exceptions.file.FileNotFoundException;
import com.proautokimium.api.Infrastructure.exceptions.file.FileStorageException;
import com.proautokimium.api.Infrastructure.repositories.gallery.GalleryDocumentRepository;
import com.proautokimium.api.Infrastructure.services.authentication.AuthorizationService;
import com.proautokimium.api.Infrastructure.services.storage.GalleryStorageService;
import com.proautokimium.api.domain.entities.auth.User;
import com.proautokimium.api.domain.entities.gallery.GalleryDocument;
import com.proautokimium.api.domain.exceptions.auth.UserNotFoundException;
import jakarta.transaction.Transactional;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Clock;
import java.util.List;
import java.util.UUID;

@Service
public class GalleryDocumentService {

    private final GalleryStorageService storageService;
    private final GalleryDocumentRepository repository;
    private final AuthorizationService authorizationService;
    private final Clock clock;

    public GalleryDocumentService(GalleryStorageService storageService, GalleryDocumentRepository repository, AuthorizationService authorizationService, Clock clock) {
        this.storageService = storageService;
        this.repository = repository;
        this.authorizationService = authorizationService;
        this.clock = clock;
    }

    @Transactional
    public GalleryDocumentResponseDTO create(MultipartFile file, CreateGalleryDocumentDTO dto, String login) {
        if(file.isEmpty())
            throw new FileNotFoundException("Nenhum arquivo enviado.");

        User user = (User) authorizationService.loadUserByUsername(login);

        if(user == null) throw new UserNotFoundException();

        try {
           String storagePath = storageService.save(file, "gallery");

            GalleryDocument document = new GalleryDocument(
                    dto.title(),
                    dto.description(),
                    dto.category(),
                    storagePath,
                    file.getOriginalFilename(),
                    file.getContentType(),
                    user,
                    clock
            );
            GalleryDocument response = repository.save(document);
            return toDto(response);
        } catch (IOException e) {
            throw new FileStorageException();
        }
    }

    public List<GalleryDocumentResponseDTO> list(){
        return repository.findAllByOrderByCreatedAtDesc()
                .stream()
                .map(this::toDto)
                .toList();
    }

    public GalleryDocument findById(UUID id) {
        return repository.findById(id)
                .orElseThrow(FileNotFoundException::new);
    }

    public byte[] getFile(UUID id){
        GalleryDocument document = repository.findById(id)
                .orElseThrow(FileNotFoundException::new);

        Path path = storageService.searchFile(document.getStoragePath());
        try{
            return Files.readAllBytes(path);
        }catch(IOException e){
            throw new FileStorageException();
        }
    }

    @Transactional
    public void delete(UUID id){
        GalleryDocument document = repository.findById(id)
                .orElseThrow(FileNotFoundException::new);

        Path path = storageService.searchFile(document.getStoragePath());
        try{
            Files.delete(path);
            repository.delete(document);
        }catch (IOException e){
            throw new FileStorageException();
        }
    }

    private GalleryDocumentResponseDTO toDto(GalleryDocument document){
        return new GalleryDocumentResponseDTO(
                document.getId(),
                document.getTitle(),
                document.getDescription(),
                document.getCategory(),
                document.getOriginalFileName(),
                document.getContentType(),
                document.getCreatedAt()
        );
    }
}
