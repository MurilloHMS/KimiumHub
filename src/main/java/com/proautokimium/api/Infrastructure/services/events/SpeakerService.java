package com.proautokimium.api.Infrastructure.services.events;

import com.proautokimium.api.Application.DTOs.events.EventDTOs.SpeakerDTO;
import com.proautokimium.api.Application.DTOs.events.EventDTOs.SpeakerRequestDTO;
import com.proautokimium.api.Infrastructure.exceptions.events.EventExceptions.EventConflictException;
import com.proautokimium.api.Infrastructure.exceptions.events.EventExceptions.SpeakerNotFoundException;
import com.proautokimium.api.Infrastructure.repositories.events.EventTalkRepository;
import com.proautokimium.api.Infrastructure.repositories.events.SpeakerRepository;
import com.proautokimium.api.Infrastructure.services.storage.EventImageStorageService;
import com.proautokimium.api.domain.entities.events.Speaker;
import jakarta.transaction.Transactional;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/** Os palestrantes: cadastrados uma vez, usados em qualquer evento. */
@Service
public class SpeakerService {

    private final SpeakerRepository speakerRepository;
    private final EventTalkRepository talkRepository;
    private final EventImageStorageService imageStorage;
    private final Clock clock;

    public SpeakerService(SpeakerRepository speakerRepository,
                          EventTalkRepository talkRepository,
                          EventImageStorageService imageStorage,
                          Clock clock) {
        this.speakerRepository = speakerRepository;
        this.talkRepository = talkRepository;
        this.imageStorage = imageStorage;
        this.clock = clock;
    }

    public List<SpeakerDTO> list() {
        Map<UUID, Long> counts = talkCounts();
        return speakerRepository.findAllByOrderByNameAsc().stream()
                .map(s -> EventMapper.speaker(s, counts))
                .toList();
    }

    @Transactional
    public SpeakerDTO create(SpeakerRequestDTO dto, MultipartFile photo, String author) throws IOException {
        Speaker speaker = new Speaker();
        speaker.setCreatedAt(LocalDateTime.now(clock));
        apply(speaker, dto, author);
        speaker = speakerRepository.save(speaker);

        if (hasFile(photo)) {
            speaker.setPhotoUrl(imageStorage.saveImage(photo, "speaker"));
        }
        return EventMapper.speaker(speaker, talkCounts());
    }

    @Transactional
    public SpeakerDTO update(UUID id, SpeakerRequestDTO dto, MultipartFile photo, String author) throws IOException {
        Speaker speaker = speakerRepository.findById(id).orElseThrow(SpeakerNotFoundException::new);
        apply(speaker, dto, author);

        String anterior = speaker.getPhotoUrl();
        if (hasFile(photo)) {
            speaker.setPhotoUrl(imageStorage.saveImage(photo, "speaker"));
        } else if (dto.removePhoto()) {
            speaker.setPhotoUrl(null);
        }

        speakerRepository.save(speaker);

        // Por último: o disco não participa da transação, e apagar antes deixaria
        // a linha apontando para um arquivo que sumiu se o banco desfizesse.
        if (anterior != null && !anterior.equals(speaker.getPhotoUrl())) {
            imageStorage.deleteByUrl(anterior);
        }
        return EventMapper.speaker(speaker, talkCounts());
    }

    /**
     * Palestrante que está em alguma palestra não se apaga.
     *
     * <p>A recusa diz em quais eventos, para quem cadastra saber onde trocar. A
     * FK sem cascade na V106 é a trava de baixo; esta é a que explica.
     */
    @Transactional
    public void delete(UUID id) throws IOException {
        Speaker speaker = speakerRepository.findById(id).orElseThrow(SpeakerNotFoundException::new);

        List<String> eventos = talkRepository.eventNamesWithSpeaker(id);
        if (!eventos.isEmpty()) {
            throw new EventConflictException(
                    speaker.getName() + " está na programação de: " + String.join(", ", eventos)
                            + ". Tire da palestra antes de excluir.");
        }

        String foto = speaker.getPhotoUrl();
        speakerRepository.delete(speaker);
        imageStorage.deleteByUrl(foto);
    }

    private void apply(Speaker speaker, SpeakerRequestDTO dto, String author) {
        speaker.setName(dto.name().trim());
        speaker.setRole(clean(dto.role()));
        speaker.setCompanyName(clean(dto.companyName()));
        speaker.setInstagram(handle(dto.instagram()));
        speaker.setLinkedin(handle(dto.linkedin()));
        speaker.setWebsite(clean(dto.website()));
        speaker.setUpdatedAt(LocalDateTime.now(clock));
        speaker.setUpdatedBy(author);
    }

    /**
     * Guarda só o usuário: {@code https://www.instagram.com/marina.quimica/} e
     * {@code @marina.quimica} viram {@code marina.quimica}. O site monta o link.
     */
    static String handle(String valor) {
        String v = clean(valor);
        if (v == null) return null;
        v = v.replaceFirst("(?i)^https?://", "")
                .replaceFirst("(?i)^(www\\.)?(instagram\\.com|linkedin\\.com)/(in/)?", "")
                .replaceFirst("^@", "");
        int corte = v.indexOf('?');
        if (corte >= 0) v = v.substring(0, corte);
        v = v.replaceAll("/+$", "");
        return v.isBlank() ? null : v;
    }

    private Map<UUID, Long> talkCounts() {
        Map<UUID, Long> mapa = new HashMap<>();
        for (Object[] linha : talkRepository.countBySpeaker()) {
            mapa.put((UUID) linha[0], ((Number) linha[1]).longValue());
        }
        return mapa;
    }

    private static boolean hasFile(MultipartFile file) {
        return file != null && !file.isEmpty();
    }

    private static String clean(String valor) {
        return valor == null || valor.isBlank() ? null : valor.trim();
    }
}
