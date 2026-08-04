package com.proautokimium.api.Infrastructure.repositories.gallery;

import com.proautokimium.api.domain.entities.gallery.GalleryDocument;
import com.proautokimium.api.domain.enums.gallery.Category;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface GalleryDocumentRepository extends JpaRepository<GalleryDocument, UUID> {
    List<GalleryDocument> findByCategory(Category caregory);
    List<GalleryDocument> findAllByOrderByCreatedAtDesc();

    UUID id(UUID id);
}
