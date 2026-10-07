package com.proautokimium.api.Infrastructure.services.email.delivery;

import com.proautokimium.api.Infrastructure.repositories.email.EmailQueueRepository;
import com.proautokimium.api.Infrastructure.services.email.delivery.LocawebReportClient.ReportPage;
import com.proautokimium.api.Infrastructure.services.email.delivery.LocawebReportClient.ReportedMessage;
import com.proautokimium.api.domain.entities.email.EmailQueue;
import com.proautokimium.api.domain.enums.EmailStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.*;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/** Casar o relatório da Locaweb com a fila, pelo tracking_id que volta em x_smtplw. */
class EmailDeliveryTrackingServiceTest {

    private static final ZoneId SP = ZoneId.of("America/Sao_Paulo");
    private static final LocalDateTime AGORA = LocalDateTime.of(2026, 10, 7, 15, 0);

    private final EmailQueueRepository repository = mock(EmailQueueRepository.class);
    private final LocawebReportClient client = mock(LocawebReportClient.class);
    private EmailDeliveryTrackingService service;

    @BeforeEach
    void setUp() {
        service = new EmailDeliveryTrackingService(repository, client, Clock.fixed(AGORA.atZone(SP).toInstant(), SP));
        when(client.isEnabled()).thenReturn(true);
        when(repository.markDelivered(any(), any())).thenReturn(1);
        when(repository.markBounced(any(), any(), any())).thenReturn(1);
    }

    private static EmailQueueRepository.AwaitingDelivery esperando(UUID id, UUID tag, LocalDateTime sentAt) {
        return new EmailQueueRepository.AwaitingDelivery() {
            public UUID getId() { return id; }
            public UUID getTrackingId() { return tag; }
            public LocalDateTime getSentAt() { return sentAt; }
        };
    }

    private static ReportedMessage entregue(UUID tag, String createdAt) {
        return new ReportedMessage(tag == null ? null : tag.toString(), "Entregue", OffsetDateTime.parse(createdAt), null, null, "");
    }

    @Test
    @DisplayName("entregue: grava a hora do registro da Locaweb, no fuso da aplicação")
    void entregue() {
        UUID id = UUID.randomUUID(), tag = UUID.randomUUID();
        when(repository.findAwaitingDelivery(eq(EmailStatus.SENT), eq(AGORA.minus(EmailQueue.DELIVERY_WINDOW))))
                .thenReturn(List.of(esperando(id, tag, AGORA.minusMinutes(9))));
        when(client.messages(LocalDate.of(2026, 10, 7), LocalDate.of(2026, 10, 7), 1))
                .thenReturn(new ReportPage(List.of(entregue(UUID.randomUUID(), "2026-10-07T14:50:00-03:00"),
                        entregue(tag, "2026-10-07T17:51:02Z")), false));

        EmailDeliveryTrackingService.Result r = service.track();

        verify(repository).markDelivered(id, LocalDateTime.of(2026, 10, 7, 14, 51, 2));
        verify(repository, times(1)).markDelivered(any(), any());
        assertThat(r.delivered()).isEqualTo(1);
    }

    @Test
    @DisplayName("devolvido: grava a hora e o motivo, e não marca como entregue")
    void devolvido() {
        UUID id = UUID.randomUUID(), tag = UUID.randomUUID();
        when(repository.findAwaitingDelivery(any(), any())).thenReturn(List.of(esperando(id, tag, AGORA.minusHours(1))));
        when(client.messages(any(), any(), eq(1))).thenReturn(new ReportPage(List.of(new ReportedMessage(tag.toString(),
                "Entregue", OffsetDateTime.parse("2026-10-07T14:00:01-03:00"), OffsetDateTime.parse("2026-10-07T14:05:00-03:00"),
                "550", "Mailbox unavailable")), false));

        service.track();

        verify(repository).markBounced(id, LocalDateTime.of(2026, 10, 7, 14, 5), "Mailbox unavailable");
        verify(repository, never()).markDelivered(any(), any());
    }

    @Test
    @DisplayName("pagina até achar todos os esperados, e para aí")
    void paginaAteAcharTodos() {
        UUID id = UUID.randomUUID(), tag = UUID.randomUUID();
        when(repository.findAwaitingDelivery(any(), any())).thenReturn(List.of(esperando(id, tag, AGORA.minusDays(2))));
        when(client.messages(LocalDate.of(2026, 10, 5), LocalDate.of(2026, 10, 7), 1))
                .thenReturn(new ReportPage(List.of(entregue(UUID.randomUUID(), "2026-10-05T10:00:00-03:00")), true));
        when(client.messages(LocalDate.of(2026, 10, 5), LocalDate.of(2026, 10, 7), 2))
                .thenReturn(new ReportPage(List.of(entregue(tag, "2026-10-05T10:00:01-03:00")), true));

        EmailDeliveryTrackingService.Result r = service.track();

        assertThat(r.pages()).isEqualTo(2);
        verify(client, never()).messages(any(), any(), eq(3));
        verify(repository).markDelivered(eq(id), any());
    }

    @Test
    @DisplayName("status que não é entrega nem devolução fica para a próxima passada")
    void statusDesconhecido() {
        UUID tag = UUID.randomUUID();
        when(repository.findAwaitingDelivery(any(), any())).thenReturn(List.of(esperando(UUID.randomUUID(), tag, AGORA)));
        when(client.messages(any(), any(), eq(1))).thenReturn(new ReportPage(List.of(new ReportedMessage(tag.toString(),
                "Enfileirado", OffsetDateTime.parse("2026-10-07T15:00:00-03:00"), null, null, null)), false));

        service.track();

        verify(repository, never()).markDelivered(any(), any());
        verify(repository, never()).markBounced(any(), any(), any());
    }

    @Test
    @DisplayName("ninguém esperando: nem chama a Locaweb")
    void ninguemEsperando() {
        when(repository.findAwaitingDelivery(any(), any())).thenReturn(List.of());

        service.track();

        verify(client, never()).messages(any(), any(), anyInt());
    }

    @Test
    @DisplayName("sem token: nem consulta o banco")
    void desligado() {
        when(client.isEnabled()).thenReturn(false);

        service.track();

        verifyNoInteractions(repository);
        verify(client, never()).messages(any(), any(), anyInt());
    }
}
