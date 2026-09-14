package com.proautokimium.api.domain.entities.events;

import com.proautokimium.api.domain.abstractions.Entity;
import com.proautokimium.api.domain.entities.humanResources.Company;
import com.proautokimium.api.domain.enums.events.TalkLocationType;
import com.proautokimium.api.domain.valueObjects.Address;
import jakarta.persistence.Column;
import jakarta.persistence.Embedded;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OrderColumn;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Um item da programação: palestra, painel, abertura, coffee break.
 *
 * <p>Sem palestrante é válido — o intervalo entra na grade. Com vários, a ordem
 * é a em que foram escolhidos ({@code position}).
 */
@jakarta.persistence.Entity
@Table(name = "event_talks")
@Getter
@Setter
@NoArgsConstructor
public class EventTalk extends Entity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "event_id", nullable = false)
    private CompanyEvent event;

    @Column(name = "title", length = 200, nullable = false)
    private String title;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @Column(name = "talk_date", nullable = false)
    private LocalDate date;

    @Column(name = "start_time", nullable = false)
    private LocalTime startTime;

    @Column(name = "end_time", nullable = false)
    private LocalTime endTime;

    @Column(name = "room", length = 100)
    private String room;

    @Enumerated(EnumType.STRING)
    @Column(name = "location_type", length = 20, nullable = false)
    private TalkLocationType locationType = TalkLocationType.EVENT;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "company_id")
    private Company company;

    @Column(name = "place_name", length = 150)
    private String placeName;

    @Embedded
    private Address address;

    @ManyToMany
    @JoinTable(name = "event_talk_speakers",
            joinColumns = @JoinColumn(name = "talk_id"),
            inverseJoinColumns = @JoinColumn(name = "speaker_id"))
    @OrderColumn(name = "position")
    private List<Speaker> speakers = new ArrayList<>();
}
