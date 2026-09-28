package com.proautokimium.api.Infrastructure.services.humanResources;

import com.proautokimium.api.Infrastructure.exceptions.humanResources.ReportRecipientAlreadyExistsException;
import com.proautokimium.api.Infrastructure.exceptions.humanResources.ReportRecipientNotFoundException;
import com.proautokimium.api.Infrastructure.repositories.humanResources.HrReportRecipientRepository;
import com.proautokimium.api.domain.entities.humanResources.HrReportRecipient;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class HrReportRecipientServiceTest {

    @Mock HrReportRecipientRepository repository;
    private HrReportRecipientService service;

    @BeforeEach
    void setUp() {
        service = new HrReportRecipientService(repository,
                Clock.fixed(Instant.parse("2026-09-28T13:00:00Z"), ZoneId.of("America/Sao_Paulo")));
    }

    @Test
    @DisplayName("grava normalizado e com quem cadastrou")
    void adiciona() {
        when(repository.existsByEmail("rh@proautokimium.com.br")).thenReturn(false);
        when(repository.save(any(HrReportRecipient.class))).thenAnswer(inv -> inv.getArgument(0));

        service.add(" RH@proautokimium.com.br", "carla.rh");

        ArgumentCaptor<HrReportRecipient> salvo = ArgumentCaptor.forClass(HrReportRecipient.class);
        verify(repository).save(salvo.capture());
        assertThat(salvo.getValue().getEmail()).isEqualTo("rh@proautokimium.com.br");
        assertThat(salvo.getValue().getCreatedBy()).isEqualTo("carla.rh");
    }

    /** A checagem compara o NORMALIZADO: senão "RH@" passaria por cima de "rh@" e bateria no UNIQUE (500). */
    @Test
    @DisplayName("o mesmo e-mail com outra caixa é recusado com 409")
    void duplicado() {
        when(repository.existsByEmail("rh@proautokimium.com.br")).thenReturn(true);

        assertThrows(ReportRecipientAlreadyExistsException.class,
                () -> service.add("RH@proautokimium.com.br", "carla.rh"));
        verify(repository, never()).save(any());
    }

    @Test
    @DisplayName("remover quem não existe é 404")
    void removerInexistente() {
        UUID id = UUID.randomUUID();
        when(repository.existsById(id)).thenReturn(false);

        assertThrows(ReportRecipientNotFoundException.class, () -> service.remove(id));
        verify(repository, never()).deleteById(any());
    }
}
