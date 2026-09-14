package com.proautokimium.api.domain.entities.events;

import com.proautokimium.api.domain.abstractions.Entity;
import com.proautokimium.api.domain.entities.humanResources.Company;
import com.proautokimium.api.domain.enums.events.EventLocationType;
import com.proautokimium.api.domain.valueObjects.Address;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Embedded;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Um evento da empresa: a Poseidon Week, de 22 a 25.
 *
 * <p>{@code Company} no nome porque {@code Event} sozinho briga com metade das
 * classes do Spring ({@code ApplicationEvent}, {@code @EventListener}) na hora de
 * importar.
 *
 * <p><b>Os dias saem do intervalo</b> {@link #startDate} a {@link #endDate}; não
 * há tabela de dias.
 *
 * <p><b>{@link #publishedAt} nulo é rascunho</b>, e rascunho não aparece em
 * Documentos.
 */
@jakarta.persistence.Entity
@Table(name = "company_events")
@Getter
@Setter
@NoArgsConstructor
public class CompanyEvent extends Entity {

    @Column(name = "name", length = 150, nullable = false)
    private String name;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @Column(name = "start_date", nullable = false)
    private LocalDate startDate;

    @Column(name = "end_date", nullable = false)
    private LocalDate endDate;

    @Enumerated(EnumType.STRING)
    @Column(name = "location_type", length = 20)
    private EventLocationType locationType;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "company_id")
    private Company company;

    @Column(name = "place_name", length = 150)
    private String placeName;

    @Embedded
    private Address address;

    @Column(name = "cover_url", length = 255)
    private String coverUrl;

    @Column(name = "published_at")
    private LocalDateTime publishedAt;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @Column(name = "updated_by", length = 100)
    private String updatedBy;

    @OneToMany(mappedBy = "event", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("date ASC, startTime ASC")
    private List<EventTalk> talks = new ArrayList<>();

    public boolean isPublished() {
        return publishedAt != null;
    }

    /** Inclusivo nas duas pontas: a palestra do último dia está dentro. */
    public boolean covers(LocalDate date) {
        return date != null && !date.isBefore(startDate) && !date.isAfter(endDate);
    }
}
