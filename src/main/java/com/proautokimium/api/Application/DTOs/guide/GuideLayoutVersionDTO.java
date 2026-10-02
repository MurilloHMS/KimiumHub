package com.proautokimium.api.Application.DTOs.guide;

import com.proautokimium.api.domain.entities.guide.GuideLayout;
import com.proautokimium.api.domain.enums.guide.GuideLayoutStatus;

import java.time.LocalDateTime;
import java.util.UUID;

/** Uma linha da lista de versões. Sem o documento: ele só viaja ao restaurar. */
public record GuideLayoutVersionDTO(
        UUID id,
        int version,
        String note,
        LocalDateTime publishedAt,
        String publishedBy,
        boolean current
) {
    public static GuideLayoutVersionDTO from(GuideLayout layout) {
        return new GuideLayoutVersionDTO(layout.getId(), layout.getVersion(), layout.getNote(),
                layout.getPublishedAt(), layout.getPublishedBy(), layout.getStatus() == GuideLayoutStatus.PUBLISHED);
    }
}
