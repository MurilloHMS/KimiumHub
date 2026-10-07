package com.proautokimium.api.Infrastructure.services.email;

import com.proautokimium.api.Application.DTOs.email.EmailQueueDTOs.*;
import com.proautokimium.api.Infrastructure.repositories.email.EmailQueueRepository;
import com.proautokimium.api.Infrastructure.repositories.email.EmailQueueRepository.StatRow;
import com.proautokimium.api.Infrastructure.services.email.delivery.EmailDeliveryTrackingService;
import com.proautokimium.api.domain.enums.EmailStatus;
import com.proautokimium.api.domain.enums.email.EmailFailureKind;
import com.proautokimium.api.domain.enums.email.EmailOrigin;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/** Os blocos A a H da análise, contados sobre linhas de exemplo. */
class EmailQueueInsightsServiceTest {

    private static final LocalDateTime AGORA = LocalDateTime.of(2026, 10, 7, 15, 0);

    /** Uma linha da projeção; o padrão é um e-mail rastreado que saiu em 40 s e chegou em 1 s. */
    private static final class Row implements StatRow {
        EmailOrigin origin = EmailOrigin.NEWSLETTER;
        EmailStatus status = EmailStatus.SENT;
        Integer attempts = 1;
        LocalDateTime createdAt = AGORA.minusHours(1), sentAt = createdAt.plusSeconds(40), deliveredAt = sentAt.plusSeconds(1), bouncedAt;
        UUID trackingId = UUID.randomUUID();
        String toEmail = "ana@gmail.com", lastError, bounceReason;

        public EmailOrigin getOrigin() { return origin; }
        public EmailStatus getStatus() { return status; }
        public Integer getAttempts() { return attempts; }
        public LocalDateTime getCreatedAt() { return createdAt; }
        public LocalDateTime getSentAt() { return sentAt; }
        public LocalDateTime getDeliveredAt() { return deliveredAt; }
        public LocalDateTime getBouncedAt() { return bouncedAt; }
        public UUID getTrackingId() { return trackingId; }
        public String getToEmail() { return toEmail; }
        public String getLastError() { return lastError; }
        public String getBounceReason() { return bounceReason; }

        Row to(String a) { toEmail = a; return this; }
        Row failed(String error) { status = EmailStatus.FAILED; sentAt = null; deliveredAt = null; lastError = error; attempts = 5; return this; }
        Row bounced(String reason) { deliveredAt = null; bouncedAt = sentAt.plusMinutes(2); bounceReason = reason; return this; }
        Row waiting() { deliveredAt = null; return this; }
        Row untracked() { trackingId = null; deliveredAt = null; return this; }
        Row from(EmailOrigin o) { origin = o; return this; }
        Row tookToSend(long s) { sentAt = createdAt.plusSeconds(s); if (deliveredAt != null) deliveredAt = sentAt.plusSeconds(1); return this; }
    }

    @Test
    @DisplayName("A: totais e taxa de entrega só sobre os rastreados concluídos")
    void totais() {
        List<StatRow> rows = List.of(new Row(), new Row(), new Row().waiting(), new Row().failed("timeout"),
                new Row().bounced("550 User unknown"), new Row().untracked());
        Totals t = EmailQueueInsightsService.totals(rows);

        assertThat(t.sent()).isEqualTo(5);
        assertThat(t.failed()).isEqualTo(1);
        assertThat(t.bounced()).isEqualTo(1);
        assertThat(t.deliveryRate()).as("2 entregues de 5 rastreados concluídos; o não rastreado fica fora").isEqualTo(40.0);
    }

    @Test
    @DisplayName("B: o funil conta só os rastreados, com o que ficou pelo caminho")
    void funil() {
        Row naFila = new Row().waiting(); naFila.status = EmailStatus.PENDING; naFila.sentAt = null;
        Funnel f = EmailQueueInsightsService.funnel(List.of(new Row(), new Row(), naFila, new Row().failed("x"),
                new Row().bounced("550"), new Row().waiting(), new Row().untracked()));

        assertThat(f.created()).isEqualTo(6);
        assertThat(f.sent()).isEqualTo(4);
        assertThat(f.delivered()).isEqualTo(2);
        assertThat(f.queued()).isEqualTo(1);
        assertThat(f.failed()).isEqualTo(1);
        assertThat(f.bounced()).isEqualTo(1);
        assertThat(f.unconfirmed()).isEqualTo(1);
    }

    @Test
    @DisplayName("C: mediana e 95% pela posição mais próxima, e cada tempo na sua faixa")
    void tempos() {
        List<StatRow> rows = new ArrayList<>();
        for (long s : new long[]{5, 10, 20, 40, 40, 50, 70, 200, 400, 1000}) rows.add(new Row().tookToSend(s));
        Timing t = EmailQueueInsightsService.timing(rows,
                r -> EmailQueueInsightsService.seconds(r.getCreatedAt(), r.getSentAt()), EmailQueueInsightsService.TO_SEND_EDGES);

        assertThat(t.count()).isEqualTo(10);
        assertThat(t.medianSeconds()).isEqualTo(40);
        assertThat(t.p95Seconds()).isEqualTo(1000);
        // <15 · 15–30 · 30–60 · 60–120 · 120–300 · 300–900 · 900+
        assertThat(t.buckets()).containsExactly(2L, 1L, 3L, 1L, 1L, 1L, 1L);
    }

    @Test
    @DisplayName("D: por origem, com a entrega e a mediana até sair")
    void porOrigem() {
        List<OriginInsight> o = EmailQueueInsightsService.origins(List.of(
                new Row().tookToSend(300), new Row().tookToSend(600), new Row().failed("x"),
                new Row().from(EmailOrigin.PASSWORD_RESET).tookToSend(0)));

        assertThat(o.getFirst().origin()).isEqualTo(EmailOrigin.NEWSLETTER);
        assertThat(o.getFirst().total()).isEqualTo(3);
        assertThat(o.getFirst().failed()).isEqualTo(1);
        assertThat(o.getFirst().deliveryRate()).isEqualTo(66.7);
        assertThat(o.getFirst().medianToSendSeconds()).isEqualTo(300);
        assertThat(o.get(1).label()).isEqualTo("Redefinição de senha");
    }

    @Test
    @DisplayName("E: os quatro provedores maiores pelo nome, o resto em 'outros'")
    void provedores() {
        List<StatRow> rows = new ArrayList<>();
        for (int i = 0; i < 4; i++) rows.add(new Row().to("a" + i + "@gmail.com"));
        for (int i = 0; i < 3; i++) rows.add(new Row().to("b" + i + "@Hotmail.com").bounced("550"));
        rows.add(new Row().to("c@uol.com.br"));
        rows.add(new Row().to("d@proautokimium.com.br"));
        rows.add(new Row().to("e@x.com"));
        rows.add(new Row().to("f@y.com"));

        List<DomainInsight> d = EmailQueueInsightsService.domains(rows);

        assertThat(d).extracting(DomainInsight::domain)
                .containsExactly("gmail.com", "hotmail.com", "proautokimium.com.br", "uol.com.br", "outros (2)");
        assertThat(d.get(1).bounced()).isEqualTo(3);
        assertThat(d.get(1).deliveryRate()).isEqualTo(0.0);
        assertThat(d.getLast().total()).isEqualTo(2);
    }

    @Test
    @DisplayName("F: endereço com 2 ou mais falhas ou devoluções, com o último motivo; 1 vez só não entra")
    void enderecosQueFalham() {
        Row antiga = new Row().to("joao@hotmial.com").failed("timeout"); antiga.createdAt = AGORA.minusDays(2);
        Row recente = new Row().to("JOAO@hotmial.com").failed("AddressException: invalid address");
        List<ProblemAddress> p = EmailQueueInsightsService.problemAddresses(List.of(
                antiga, recente, new Row().to("ana@x.com").bounced("Mailbox full, 552"), new Row().to("ana@x.com").bounced("552"),
                new Row().to("uma@vez.com").failed("timeout"), new Row()));

        assertThat(p).extracting(ProblemAddress::address).containsExactly("ana@x.com", "joao@hotmial.com");
        assertThat(p.get(1).times()).isEqualTo(2);
        assertThat(p.get(1).lastKind()).as("o motivo da mais recente").isEqualTo(EmailFailureKind.INVALID_ADDRESS);
        assertThat(p.get(0).lastKind()).isEqualTo(EmailFailureKind.MAILBOX_FULL);
    }

    @Test
    @DisplayName("H: enviados por hora do envio")
    void porHora() {
        Row oito = new Row(); oito.sentAt = AGORA.withHour(8).withMinute(5);
        Row oitoEMeia = new Row(); oitoEMeia.sentAt = AGORA.withHour(8).withMinute(30);
        Row seis = new Row(); seis.sentAt = AGORA.withHour(6);
        List<Long> h = EmailQueueInsightsService.perHour(List.of(oito, oitoEMeia, seis, new Row().failed("x")));

        assertThat(h).hasSize(24);
        assertThat(h.get(8)).isEqualTo(2);
        assertThat(h.get(6)).isEqualTo(1);
        assertThat(h.stream().mapToLong(Long::longValue).sum()).isEqualTo(3);
    }

    @Test
    @DisplayName("A e G: o período anterior tem o mesmo tamanho e termina onde este começa; e a saúde do rastreio")
    void periodoAnteriorESaude() {
        ZoneId sp = ZoneId.of("America/Sao_Paulo");
        Clock clock = Clock.fixed(AGORA.atZone(sp).toInstant(), sp);
        EmailQueueRepository repo = mock(EmailQueueRepository.class);
        EmailDeliveryTrackingService tracking = mock(EmailDeliveryTrackingService.class);
        Row velha = new Row().waiting(); velha.sentAt = AGORA.minusDays(4);
        when(repo.statRows(any(), any())).thenReturn(List.of(new Row().waiting(), velha));
        when(tracking.isEnabled()).thenReturn(true);
        when(tracking.lastRun()).thenReturn(Optional.of(new EmailDeliveryTrackingService.LastRun(AGORA.minusMinutes(3), true, 1, null)));
        EmailQueueInsightsService service = new EmailQueueInsightsService(repo, new EmailQueueAdminService(repo, clock), tracking, clock);

        Insights i = service.insights(7, null);

        LocalDateTime inicio = LocalDate.of(2026, 10, 1).atStartOfDay();
        verify(repo).statRows(inicio.minusDays(7), inicio);
        verify(repo).statRows(eq(inicio), any());
        assertThat(i.tracking().awaiting()).isEqualTo(1);
        assertThat(i.tracking().unconfirmed()).as("enviado há 4 dias passou da janela de 3").isEqualTo(1);
        assertThat(i.tracking().lastRunOk()).isTrue();
        assertThat(i.tracking().lastRunAt()).isEqualTo(AGORA.minusMinutes(3));
    }

    private static LocalDateTime eq(LocalDateTime v) {
        return org.mockito.ArgumentMatchers.eq(v);
    }
}
