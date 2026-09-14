package com.proautokimium.api.Infrastructure.services.events;

import com.proautokimium.api.Application.DTOs.address.AddressDTO;
import com.proautokimium.api.Application.DTOs.events.EventDTOs.EventDetailDTO;
import com.proautokimium.api.Application.DTOs.events.EventDTOs.EventRequestDTO;
import com.proautokimium.api.Application.DTOs.events.EventDTOs.EventSummaryDTO;
import com.proautokimium.api.Application.DTOs.events.EventDTOs.TalkRequestDTO;
import com.proautokimium.api.Infrastructure.exceptions.events.EventExceptions.EventConflictException;
import com.proautokimium.api.Infrastructure.exceptions.events.EventExceptions.EventNotFoundException;
import com.proautokimium.api.Infrastructure.exceptions.events.EventExceptions.InvalidEventDataException;
import com.proautokimium.api.Infrastructure.exceptions.events.EventExceptions.SpeakerNotFoundException;
import com.proautokimium.api.Infrastructure.exceptions.events.EventExceptions.TalkNotFoundException;
import com.proautokimium.api.Infrastructure.exceptions.humanResources.CompanyNotFoundException;
import com.proautokimium.api.Infrastructure.repositories.events.CompanyEventRepository;
import com.proautokimium.api.Infrastructure.repositories.events.SpeakerRepository;
import com.proautokimium.api.Infrastructure.repositories.humanResources.CompanyRepository;
import com.proautokimium.api.Infrastructure.services.storage.EventImageStorageService;
import com.proautokimium.api.domain.entities.events.CompanyEvent;
import com.proautokimium.api.domain.entities.events.EventTalk;
import com.proautokimium.api.domain.entities.events.Speaker;
import com.proautokimium.api.domain.entities.humanResources.Company;
import com.proautokimium.api.domain.enums.events.EventLocationType;
import com.proautokimium.api.domain.enums.events.TalkLocationType;
import com.proautokimium.api.domain.valueObjects.Address;
import jakarta.transaction.Transactional;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.Clock;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Os eventos e a programação deles.
 *
 * <p>A programação mora aqui, e não num service próprio, porque toda regra dela
 * pergunta ao evento: a data da palestra precisa caber no intervalo, e encurtar o
 * intervalo precisa olhar as palestras.
 */
@Service
public class CompanyEventService {

    private static final DateTimeFormatter DIA = DateTimeFormatter.ofPattern("dd/MM");

    private final CompanyEventRepository eventRepository;
    private final SpeakerRepository speakerRepository;
    private final CompanyRepository companyRepository;
    private final EventImageStorageService imageStorage;
    private final Clock clock;

    public CompanyEventService(CompanyEventRepository eventRepository,
                               SpeakerRepository speakerRepository,
                               CompanyRepository companyRepository,
                               EventImageStorageService imageStorage,
                               Clock clock) {
        this.eventRepository = eventRepository;
        this.speakerRepository = speakerRepository;
        this.companyRepository = companyRepository;
        this.imageStorage = imageStorage;
        this.clock = clock;
    }

    // ─── Leitura ─────────────────────────────────────────────────────────────

    /** Documentos: só o que foi publicado. */
    @Transactional
    public List<EventSummaryDTO> listPublished() {
        return eventRepository.findPublished().stream().map(EventMapper::summary).toList();
    }

    /** Cadastro: tudo, rascunho incluído. */
    @Transactional
    public List<EventSummaryDTO> listForManagement() {
        return eventRepository.findAllForManagement().stream().map(EventMapper::summary).toList();
    }

    /**
     * Um evento com a programação.
     *
     * <p><b>Rascunho responde 404 para quem só vê.</b> Não 403: dizer "existe,
     * mas você não pode" já conta que há um evento sendo montado. Quem cadastra
     * abre o rascunho pelo "Ver como fica".
     */
    @Transactional
    public EventDetailDTO get(UUID id, boolean canSeeDrafts) {
        CompanyEvent event = find(id);
        if (!event.isPublished() && !canSeeDrafts) {
            throw new EventNotFoundException();
        }
        return EventMapper.detail(event);
    }

    // ─── Evento ──────────────────────────────────────────────────────────────

    /** Nasce rascunho, sempre. Publicar é um passo separado. */
    @Transactional
    public EventDetailDTO create(EventRequestDTO dto, MultipartFile cover, String author) throws IOException {
        CompanyEvent event = new CompanyEvent();
        event.setCreatedAt(LocalDateTime.now(clock));
        apply(event, dto, author);
        event = eventRepository.save(event);

        if (hasFile(cover)) {
            event.setCoverUrl(imageStorage.saveImage(cover, "event"));
        }
        return EventMapper.detail(event);
    }

    @Transactional
    public EventDetailDTO update(UUID id, EventRequestDTO dto, MultipartFile cover, String author) throws IOException {
        CompanyEvent event = find(id);

        refuseTalksOutside(event, dto);
        apply(event, dto, author);

        String anterior = event.getCoverUrl();
        if (hasFile(cover)) {
            event.setCoverUrl(imageStorage.saveImage(cover, "event"));
        } else if (dto.removeCover()) {
            event.setCoverUrl(null);
        }

        eventRepository.save(event);

        if (anterior != null && !anterior.equals(event.getCoverUrl())) {
            imageStorage.deleteByUrl(anterior);
        }
        return EventMapper.detail(event);
    }

    @Transactional
    public void delete(UUID id) throws IOException {
        CompanyEvent event = find(id);
        String capa = event.getCoverUrl();
        eventRepository.delete(event);
        imageStorage.deleteByUrl(capa);
    }

    /** Publicar de novo não muda a data: a primeira publicação é a que conta. */
    @Transactional
    public EventDetailDTO publish(UUID id, String author) {
        CompanyEvent event = find(id);
        if (!event.isPublished()) {
            event.setPublishedAt(LocalDateTime.now(clock));
            touch(event, author);
        }
        return EventMapper.detail(eventRepository.saveAndFlush(event));
    }

    @Transactional
    public EventDetailDTO unpublish(UUID id, String author) {
        CompanyEvent event = find(id);
        if (event.isPublished()) {
            event.setPublishedAt(null);
            touch(event, author);
        }
        return EventMapper.detail(eventRepository.saveAndFlush(event));
    }

    // ─── Programação ─────────────────────────────────────────────────────────

    @Transactional
    public EventDetailDTO addTalk(UUID eventId, TalkRequestDTO dto, String author) {
        CompanyEvent event = find(eventId);
        EventTalk talk = new EventTalk();
        talk.setEvent(event);
        applyTalk(event, talk, dto);
        event.getTalks().add(talk);
        touch(event, author);
        return EventMapper.detail(eventRepository.saveAndFlush(event));
    }

    @Transactional
    public EventDetailDTO updateTalk(UUID eventId, UUID talkId, TalkRequestDTO dto, String author) {
        CompanyEvent event = find(eventId);
        EventTalk talk = findTalk(event, talkId);
        applyTalk(event, talk, dto);
        touch(event, author);
        return EventMapper.detail(eventRepository.saveAndFlush(event));
    }

    @Transactional
    public EventDetailDTO deleteTalk(UUID eventId, UUID talkId, String author) {
        CompanyEvent event = find(eventId);
        EventTalk talk = findTalk(event, talkId);
        event.getTalks().remove(talk);
        touch(event, author);
        return EventMapper.detail(eventRepository.saveAndFlush(event));
    }

    // ─── Regras ──────────────────────────────────────────────────────────────

    private void apply(CompanyEvent event, EventRequestDTO dto, String author) {
        if (dto.endDate().isBefore(dto.startDate())) {
            throw new InvalidEventDataException("O último dia não pode ser antes do primeiro.");
        }

        event.setName(dto.name().trim());
        event.setDescription(clean(dto.description()));
        event.setStartDate(dto.startDate());
        event.setEndDate(dto.endDate());

        EventLocationType type = dto.locationType();
        event.setLocationType(type);
        event.setCompany(null);
        event.setPlaceName(null);
        event.setAddress(null);

        if (type == EventLocationType.COMPANY) {
            event.setCompany(requireCompany(dto.companyId()));
        } else if (type == EventLocationType.ADDRESS) {
            event.setPlaceName(requirePlace(dto.placeName()));
            event.setAddress(requireAddress(dto.address()));
        }

        touch(event, author);
    }

    /**
     * Encurtar o evento não pode deixar palestra fora dos dias.
     *
     * <p>Recusa em vez de apagar ou mover: as duas saídas "automáticas" mexem na
     * programação de alguém sem avisar. A mensagem diz quantas e quais dias.
     */
    private static void refuseTalksOutside(CompanyEvent event, EventRequestDTO dto) {
        CompanyEvent novo = new CompanyEvent();
        novo.setStartDate(dto.startDate());
        novo.setEndDate(dto.endDate());

        List<EventTalk> fora = event.getTalks().stream()
                .filter(t -> !novo.covers(t.getDate()))
                .toList();

        if (!fora.isEmpty()) {
            String dias = fora.stream().map(t -> t.getDate().format(DIA))
                    .distinct()
                    .collect(Collectors.joining(", "));
            throw new EventConflictException(fora.size() == 1
                    ? "1 palestra ficaria fora dos dias novos (" + dias + "). Mude a data dela antes."
                    : fora.size() + " palestras ficariam fora dos dias novos (" + dias + "). Mude as datas delas antes.");
        }
    }

    private void applyTalk(CompanyEvent event, EventTalk talk, TalkRequestDTO dto) {
        if (!event.covers(dto.date())) {
            throw new InvalidEventDataException("O dia da palestra precisa estar entre "
                    + event.getStartDate().format(DIA) + " e " + event.getEndDate().format(DIA) + ".");
        }
        if (!dto.endTime().isAfter(dto.startTime())) {
            throw new InvalidEventDataException("O horário de fim precisa ser depois do início.");
        }

        talk.setTitle(dto.title().trim());
        talk.setDescription(clean(dto.description()));
        talk.setDate(dto.date());
        talk.setStartTime(dto.startTime());
        talk.setEndTime(dto.endTime());
        talk.setRoom(clean(dto.room()));

        TalkLocationType type = dto.locationType() == null ? TalkLocationType.EVENT : dto.locationType();
        talk.setLocationType(type);
        talk.setCompany(null);
        talk.setPlaceName(null);
        talk.setAddress(null);

        if (type == TalkLocationType.COMPANY) {
            talk.setCompany(requireCompany(dto.companyId()));
        } else if (type == TalkLocationType.ADDRESS) {
            talk.setPlaceName(requirePlace(dto.placeName()));
            talk.setAddress(requireAddress(dto.address()));
        }

        talk.getSpeakers().clear();
        talk.getSpeakers().addAll(resolveSpeakers(dto.speakerIds()));
    }

    /** Na ordem pedida, sem repetir, e recusando id que não existe. */
    private List<Speaker> resolveSpeakers(List<UUID> ids) {
        if (ids == null || ids.isEmpty()) {
            return List.of();
        }
        List<UUID> unicos = new ArrayList<>(new LinkedHashSet<>(ids));
        Map<UUID, Speaker> encontrados = speakerRepository.findAllById(unicos).stream()
                .collect(Collectors.toMap(Speaker::getId, Function.identity()));

        List<Speaker> ordenados = new ArrayList<>();
        for (UUID id : unicos) {
            Speaker s = encontrados.get(id);
            if (s == null) {
                throw new SpeakerNotFoundException();
            }
            ordenados.add(s);
        }
        return ordenados;
    }

    private Company requireCompany(UUID id) {
        if (id == null) {
            throw new InvalidEventDataException("Escolha a empresa do grupo.");
        }
        return companyRepository.findById(id).orElseThrow(CompanyNotFoundException::new);
    }

    private static String requirePlace(String name) {
        String nome = clean(name);
        if (nome == null) {
            throw new InvalidEventDataException("Informe o nome do lugar.");
        }
        return nome;
    }

    /** Sem rua e cidade o mapa e o Waze não têm o que procurar. */
    private static Address requireAddress(AddressDTO dto) {
        Address address = dto == null ? null : dto.toAddress();
        if (address == null || !address.isUsable()) {
            throw new InvalidEventDataException("Informe pelo menos a rua e a cidade do endereço.");
        }
        return address;
    }

    private CompanyEvent find(UUID id) {
        return eventRepository.findById(id).orElseThrow(EventNotFoundException::new);
    }

    private static EventTalk findTalk(CompanyEvent event, UUID talkId) {
        return event.getTalks().stream()
                .filter(t -> talkId.equals(t.getId()))
                .findFirst()
                .orElseThrow(TalkNotFoundException::new);
    }

    private void touch(CompanyEvent event, String author) {
        event.setUpdatedAt(LocalDateTime.now(clock));
        event.setUpdatedBy(author);
    }

    private static boolean hasFile(MultipartFile file) {
        return file != null && !file.isEmpty();
    }

    private static String clean(String valor) {
        return valor == null || valor.isBlank() ? null : valor.trim();
    }
}
