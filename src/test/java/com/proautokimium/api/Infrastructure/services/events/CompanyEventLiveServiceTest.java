package com.proautokimium.api.Infrastructure.services.events;

import com.proautokimium.api.Application.DTOs.events.EventDTOs.EventDetailDTO;
import com.proautokimium.api.Application.DTOs.events.EventDTOs.EventRequestDTO;
import com.proautokimium.api.Infrastructure.exceptions.events.EventExceptions.InvalidEventDataException;
import com.proautokimium.api.Infrastructure.repositories.EmployeeRepository;
import com.proautokimium.api.Infrastructure.repositories.events.CompanyEventRepository;
import com.proautokimium.api.Infrastructure.repositories.events.SpeakerRepository;
import com.proautokimium.api.Infrastructure.repositories.humanResources.CompanyRepository;
import com.proautokimium.api.Infrastructure.repositories.humanResources.DepartmentRepository;
import com.proautokimium.api.Infrastructure.services.storage.EventImageStorageService;
import com.proautokimium.api.domain.abstractions.Entity;
import com.proautokimium.api.domain.entities.events.CompanyEvent;
import com.proautokimium.api.domain.entities.events.EventTalk;
import com.proautokimium.api.domain.entities.humanResources.Company;
import com.proautokimium.api.domain.enums.events.EventLocationType;
import com.proautokimium.api.domain.valueObjects.Address;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.lang.reflect.Field;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * O cadastro da live: o link, o horário, o aviso ao publicar e o duplicar.
 */
@ExtendWith(MockitoExtension.class)
class CompanyEventLiveServiceTest {

    @Mock CompanyEventRepository eventRepository;
    @Mock SpeakerRepository speakerRepository;
    @Mock CompanyRepository companyRepository;
    @Mock DepartmentRepository departmentRepository;
    @Mock EmployeeRepository employeeRepository;
    @Mock EventImageStorageService imageStorage;
    @Mock EventAnnouncementService announcements;

    private static final Clock RELOGIO =
            Clock.fixed(Instant.parse("2026-10-06T13:00:00Z"), ZoneId.of("America/Sao_Paulo"));
    private static final LocalDate QUINTA = LocalDate.of(2026, 10, 8);
    private static final String YOUTUBE = "https://www.youtube.com/live/abc123";

    private CompanyEventService service;

    @BeforeEach
    void setUp() {
        service = new CompanyEventService(eventRepository, speakerRepository, companyRepository,
                departmentRepository, employeeRepository, imageStorage, announcements, RELOGIO);
        lenient().when(eventRepository.save(any(CompanyEvent.class))).thenAnswer(i -> comId(i.getArgument(0)));
        lenient().when(eventRepository.saveAndFlush(any(CompanyEvent.class))).thenAnswer(i -> i.getArgument(0));
    }

    // ─── O link e o horário ──────────────────────────────────────────────────

    @Test
    @DisplayName("live válida grava link e horário; o início dela vem do horário, não da meia-noite")
    void validLive() throws Exception {
        EventDetailDTO salvo = service.create(live(YOUTUBE, "09:00", "09:40"), null, "rh");

        assertThat(salvo.location().source()).isEqualTo("ONLINE");
        assertThat(salvo.location().onlineUrl()).isEqualTo(YOUTUBE);
        assertThat(salvo.startsAt()).isEqualTo(LocalDateTime.of(2026, 10, 8, 9, 0));
        assertThat(salvo.endsAt()).isEqualTo(LocalDateTime.of(2026, 10, 8, 9, 40));
        assertThat(salvo.answersUntil()).as("a live aceita confirmação até acabar").isEqualTo(salvo.endsAt());
    }

    /**
     * O link vira um botão que todo colaborador toca sem pensar. Esquema que
     * executa código ou que trafega aberto não pode chegar lá.
     */
    @ParameterizedTest
    @ValueSource(strings = {"javascript:alert(1)", "http://www.youtube.com/live/x", "www.youtube.com/live/x",
            "https://", "https://localhost/x", "ftp://arquivos.empresa.com/x"})
    @DisplayName("link que não é https de um site de verdade é recusado")
    void refusesUnsafeLinks(String url) {
        assertThatThrownBy(() -> service.create(live(url, "09:00", "09:40"), null, "rh"))
                .isInstanceOf(InvalidEventDataException.class);
        verify(eventRepository, never()).save(any());
    }

    @Test
    @DisplayName("live sem horário é recusada, com a frase que diz o que falta")
    void requiresTimes() {
        assertThatThrownBy(() -> service.create(live(YOUTUBE, null, null), null, "rh"))
                .isInstanceOf(InvalidEventDataException.class)
                .hasMessage("Informe o horário de início e de fim da transmissão.");
    }

    @Test
    @DisplayName("fim antes do início, no mesmo dia, é recusado")
    void endAfterStart() {
        assertThatThrownBy(() -> service.create(live(YOUTUBE, "10:00", "09:40"), null, "rh"))
                .isInstanceOf(InvalidEventDataException.class)
                .hasMessage("O fim da transmissão precisa ser depois do início.");
    }

    @Test
    @DisplayName("trocar de online para endereço apaga o link e o horário — não ficam escondidos no banco")
    void switchingAwayClearsOnline() throws Exception {
        CompanyEvent e = comId(new CompanyEvent());
        e.setCreatedAt(LocalDateTime.now(RELOGIO));
        e.setStartDate(QUINTA);
        e.setEndDate(QUINTA);
        e.setLocationType(EventLocationType.ONLINE);
        e.setOnlineUrl(YOUTUBE);
        e.setStartTime(LocalTime.of(9, 0));
        e.setEndTime(LocalTime.of(9, 40));
        when(eventRepository.findById(e.getId())).thenReturn(Optional.of(e));
        when(companyRepository.findById(any())).thenReturn(Optional.of(comId(new Company())));

        service.update(e.getId(), new EventRequestDTO("Alinhamento", null, QUINTA, QUINTA,
                EventLocationType.COMPANY, UUID.randomUUID(), null, null, false,
                null, null, null, null, null, null, null, YOUTUBE, LocalTime.of(9, 0), LocalTime.of(9, 40), null, null),
                null, "rh");

        assertThat(e.getOnlineUrl()).isNull();
        assertThat(e.getStartTime()).isNull();
        assertThat(e.getEndTime()).isNull();
    }

    // ─── Publicar ────────────────────────────────────────────────────────────

    @Test
    @DisplayName("publicar chama o aviso — e o evento já está gravado quando ele roda")
    void publishAnnounces() {
        CompanyEvent e = evento();
        when(eventRepository.findById(e.getId())).thenReturn(Optional.of(e));

        service.publish(e.getId(), "rh");

        verify(announcements).announcePublished(e);
        assertThat(e.isPublished()).isTrue();
        // Os convidados saem de uma consulta: avisar antes de gravar leria o
        // público velho, ou nenhum. A ordem vem do número de sequência que o
        // Mockito dá a cada chamada, nos dois dublês.
        assertThat(firstCall(eventRepository, "saveAndFlush"))
                .as("primeira gravação antes do aviso")
                .isLessThan(firstCall(announcements, "announcePublished"));
    }

    private static int firstCall(Object mock, String method) {
        return org.mockito.Mockito.mockingDetails(mock).getInvocations().stream()
                .filter(i -> i.getMethod().getName().equals(method))
                .mapToInt(org.mockito.invocation.Invocation::getSequenceNumber)
                .min().orElse(Integer.MAX_VALUE);
    }

    // ─── Duplicar ────────────────────────────────────────────────────────────

    @Test
    @DisplayName("duplicar: mesma live uma semana depois, como rascunho, sem levar os avisos já enviados")
    void duplicates() throws Exception {
        CompanyEvent original = evento();
        original.setPublishedAt(LocalDateTime.of(2026, 10, 1, 10, 0));
        original.setAnnouncedAt(LocalDateTime.of(2026, 10, 1, 10, 0));
        original.setLiveStartNotifiedAt(LocalDateTime.of(2026, 10, 8, 9, 0));
        original.setCoverUrl("/upload/events/event-capa.png");
        Address endereco = new Address("87020-900", "Av. Colombo", "5790", null, "Zona 7", "Maringá", "PR");
        original.setAddress(endereco);
        Company matriz = comId(new Company());
        original.setAudienceAll(false);
        original.getAudienceCompanies().add(matriz);
        EventTalk abertura = comId(new EventTalk());
        abertura.setEvent(original);
        abertura.setTitle("Abertura");
        abertura.setDate(QUINTA);
        abertura.setStartTime(LocalTime.of(9, 0));
        abertura.setEndTime(LocalTime.of(9, 10));
        original.getTalks().add(abertura);
        when(eventRepository.findById(original.getId())).thenReturn(Optional.of(original));
        when(imageStorage.copyByUrl("/upload/events/event-capa.png")).thenReturn("/upload/events/event-copia.png");

        EventDetailDTO copia = service.duplicate(original.getId(), "rh");

        assertThat(copia.id()).isNotEqualTo(original.getId());
        assertThat(copia.startDate()).isEqualTo(LocalDate.of(2026, 10, 15));
        assertThat(copia.location().onlineUrl()).isEqualTo(YOUTUBE);
        assertThat(copia.startTime()).isEqualTo(LocalTime.of(9, 0));
        assertThat(copia.publishedAt()).as("nasce rascunho").isNull();
        assertThat(copia.settings().announcedAt()).as("o aviso da original não conta para a cópia").isNull();
        assertThat(copia.settings().companies()).extracting(c -> c.id()).containsExactly(matriz.getId());
        assertThat(copia.talks()).singleElement().satisfies(t -> assertThat(t.date()).isEqualTo(LocalDate.of(2026, 10, 15)));
        assertThat(copia.coverUrl()).as("arquivo próprio: apagar um não apaga a capa do outro")
                .isEqualTo("/upload/events/event-copia.png");
        assertThat(original.getTalks()).as("a original continua com a palestra dela").hasSize(1);
    }

    @Test
    @DisplayName("o endereço da cópia é outro objeto: mexer num não mexe no outro")
    void duplicateCopiesAddress() throws Exception {
        CompanyEvent original = evento();
        original.setLocationType(EventLocationType.ADDRESS);
        original.setOnlineUrl(null);
        original.setAddress(new Address("87020-900", "Av. Colombo", "5790", null, "Zona 7", "Maringá", "PR"));
        when(eventRepository.findById(original.getId())).thenReturn(Optional.of(original));
        org.mockito.ArgumentCaptor<CompanyEvent> salvo = org.mockito.ArgumentCaptor.forClass(CompanyEvent.class);

        service.duplicate(original.getId(), "rh");

        verify(eventRepository).save(salvo.capture());
        assertThat(salvo.getValue().getAddress()).isEqualTo(original.getAddress()).isNotSameAs(original.getAddress());
    }

    // ─── Montagem ────────────────────────────────────────────────────────────

    private static EventRequestDTO live(String url, String start, String end) {
        return new EventRequestDTO("Alinhamento semanal", null, QUINTA, QUINTA, EventLocationType.ONLINE,
                null, null, null, false, null, null, null, null, null, null, null,
                url, start == null ? null : LocalTime.parse(start), end == null ? null : LocalTime.parse(end),
                true, true);
    }

    private CompanyEvent evento() {
        CompanyEvent e = comId(new CompanyEvent());
        e.setName("Alinhamento semanal");
        e.setCreatedAt(LocalDateTime.now(RELOGIO));
        e.setStartDate(QUINTA);
        e.setEndDate(QUINTA);
        e.setLocationType(EventLocationType.ONLINE);
        e.setOnlineUrl(YOUTUBE);
        e.setStartTime(LocalTime.of(9, 0));
        e.setEndTime(LocalTime.of(9, 40));
        e.setNotifyLiveStart(true);
        return e;
    }

    private static <T extends Entity> T comId(T entidade) {
        try {
            Field id = Entity.class.getDeclaredField("id");
            id.setAccessible(true);
            if (id.get(entidade) == null) id.set(entidade, UUID.randomUUID());
            return entidade;
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException(e);
        }
    }
}
