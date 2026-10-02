package com.proautokimium.api.Application.DTOs.guide;

import com.fasterxml.jackson.annotation.JsonRawValue;
import com.proautokimium.api.domain.entities.guide.GuideLayout;
import com.proautokimium.api.domain.enums.guide.GuideLayoutStatus;

import java.time.LocalDateTime;
import java.util.UUID;

/** O documento vai cru: é o mesmo JSON que o editor mandou, sem passar por records. */
public record GuideLayoutDTO(
        UUID id,
        GuideLayoutStatus status,
        Integer version,
        @JsonRawValue String document,
        String note,
        LocalDateTime updatedAt,
        String updatedBy,
        LocalDateTime publishedAt,
        String publishedBy
) {
    public static GuideLayoutDTO from(GuideLayout layout) {
        if (layout == null) return null;
        return new GuideLayoutDTO(layout.getId(), layout.getStatus(), layout.getVersion(), layout.getDocument(),
                layout.getNote(), layout.getUpdatedAt(), layout.getUpdatedBy(),
                layout.getPublishedAt(), layout.getPublishedBy());
    }
}
