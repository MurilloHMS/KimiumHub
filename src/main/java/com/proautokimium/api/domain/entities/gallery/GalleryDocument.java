package com.proautokimium.api.domain.entities.gallery;

import com.proautokimium.api.domain.entities.auth.User;
import com.proautokimium.api.domain.enums.gallery.Category;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

import java.time.Clock;
import java.time.LocalDateTime;

@Entity
@Table(name = "gallery_documents")
@Getter
@Setter
public class GalleryDocument extends com.proautokimium.api.domain.abstractions.Entity {
    @Column(length = 200, nullable = false)
    private String title;
    @Column(length = 500)
    private String description;
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Category category;
    @Column(name = "original_filename", length = 255, nullable = false)
    private String originalFileName;
    @Column(name = "storage_path", length = 500, nullable = false)
    private String storagePath;
    @Column(name = "content_type", length = 100, nullable = false)
    private String contentType;
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by_id", nullable = false)
    private User createdBy;
    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;


    protected GalleryDocument() { }

    public GalleryDocument(String title, String description, Category category, String storagePath, String originalFileName, String contentType, User createdBy, Clock clock){
        this.title = title;
        this.description = description;
        this.category = category;
        this.originalFileName = originalFileName;
        this.storagePath = storagePath;
        this.contentType = contentType;
        this.createdBy = createdBy;
        this.createdAt = LocalDateTime.now(clock);
    }
}
