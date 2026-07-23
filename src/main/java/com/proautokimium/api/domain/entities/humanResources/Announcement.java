package com.proautokimium.api.domain.entities.humanResources;

import com.proautokimium.api.domain.entities.Employee;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@jakarta.persistence.Entity
@Table(name = "announcements")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Announcement extends com.proautokimium.api.domain.abstractions.Entity {

    @Column(name = "title", length = 200, nullable = false)
    private String title;

    @Column(name = "content", length = 4000, nullable = false)
    private String content;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "published_by_id", nullable = false)
    private Employee publishedBy;

    @Column(name = "published_at", nullable = false)
    private LocalDateTime publishedAt;
}
