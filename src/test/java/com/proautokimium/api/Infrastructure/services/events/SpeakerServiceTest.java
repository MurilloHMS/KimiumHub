package com.proautokimium.api.Infrastructure.services.events;

import com.proautokimium.api.Application.DTOs.events.EventDTOs.SpeakerRequestDTO;
import com.proautokimium.api.Infrastructure.exceptions.events.EventExceptions.EventConflictException;
import com.proautokimium.api.Infrastructure.repositories.events.EventTalkRepository;
import com.proautokimium.api.Infrastructure.repositories.events.SpeakerRepository;
import com.proautokimium.api.Infrastructure.services.storage.EventImageStorageService;
import com.proautokimium.api.domain.abstractions.Entity;
import com.proautokimium.api.domain.entities.events.Speaker;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;

import java.lang.reflect.Field;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SpeakerServiceTest {

    @Mock SpeakerRepository speakerRepository;
    @Mock EventTalkRepository talkRepository;
    @Mock EventImageStorageService imageStorage;

    private static final Clock RELOGIO =
            Clock.fixed(Instant.parse("2026-09-14T14:30:00Z"), ZoneId.of("America/Sao_Paulo"));

    private SpeakerService service() {
        return new SpeakerService(speakerRepository, talkRepository, imageStorage, RELOGIO);
    }

    private static Speaker marina() {
        Speaker s = new Speaker();
        s.setName("Marina Alves");
        s.setPhotoUrl("/upload/events/speaker-velha.png");
        try {
            Field id = Entity.class.getDeclaredField("id");
            id.setAccessible(true);
            id.set(s, UUID.randomUUID());
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException(e);
        }
        return s;
    }

    private static SpeakerRequestDTO dados() {
        return new SpeakerRequestDTO("Marina Alves", "Química", null, "@marina.quimica", null, null, false);
    }

    /**
     * Três formatos para o mesmo perfil viram um só. Guardando a URL inteira, o
     * link montado pelo site quebraria no primeiro {@code www}.
     */
    @Test
    @DisplayName("Instagram e LinkedIn guardam so o usuario, venha como vier")
    void normalizaRedes() {
        assertThat(SpeakerService.handle("https://www.instagram.com/marina.quimica/?hl=pt")).isEqualTo("marina.quimica");
        assertThat(SpeakerService.handle("@marina.quimica")).isEqualTo("marina.quimica");
        assertThat(SpeakerService.handle("instagram.com/marina.quimica")).isEqualTo("marina.quimica");
        assertThat(SpeakerService.handle("https://www.linkedin.com/in/marinaalves/")).isEqualTo("marinaalves");
        assertThat(SpeakerService.handle("marinaalves")).isEqualTo("marinaalves");
        assertThat(SpeakerService.handle("  ")).isNull();
    }

    @Test
    @DisplayName("palestrante em palestra nao se apaga, e a recusa diz em quais eventos")
    void emUsoNaoApaga() throws Exception {
        Speaker s = marina();
        when(speakerRepository.findById(s.getId())).thenReturn(Optional.of(s));
        when(talkRepository.eventNamesWithSpeaker(s.getId())).thenReturn(List.of("Poseidon Week", "Treinamento de Aplicadores"));

        assertThatThrownBy(() -> service().delete(s.getId()))
                .isInstanceOf(EventConflictException.class)
                .hasMessage("Marina Alves está na programação de: Poseidon Week, Treinamento de Aplicadores. Tire da palestra antes de excluir.");

        verify(speakerRepository, never()).delete(any());
        verify(imageStorage, never()).deleteByUrl(anyString());
    }

    @Test
    @DisplayName("foto nova: grava a nova e so depois apaga a antiga")
    void trocaFotoApagaDepois() throws Exception {
        Speaker s = marina();
        when(speakerRepository.findById(s.getId())).thenReturn(Optional.of(s));
        MockMultipartFile foto = new MockMultipartFile("photo", "nova.png", "image/png", new byte[]{1});
        when(imageStorage.saveImage(foto, "speaker")).thenReturn("/upload/events/speaker-nova.png");

        service().update(s.getId(), dados(), foto, "murillo.henrique");

        assertThat(s.getPhotoUrl()).isEqualTo("/upload/events/speaker-nova.png");
        assertThat(s.getInstagram()).isEqualTo("marina.quimica");
        InOrder ordem = inOrder(speakerRepository, imageStorage);
        ordem.verify(speakerRepository).save(s);
        ordem.verify(imageStorage).deleteByUrl("/upload/events/speaker-velha.png");
    }

    @Test
    @DisplayName("salvar sem foto e sem remover mantem a foto")
    void semFotoMantem() throws Exception {
        Speaker s = marina();
        when(speakerRepository.findById(s.getId())).thenReturn(Optional.of(s));

        service().update(s.getId(), dados(), null, "x");

        assertThat(s.getPhotoUrl()).isEqualTo("/upload/events/speaker-velha.png");
        verify(imageStorage, never()).deleteByUrl(anyString());
    }
}
