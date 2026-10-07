package com.proautokimium.api.Infrastructure.services.email;

import com.proautokimium.api.Application.DTOs.email.EmailQueueDTOs.*;
import com.proautokimium.api.Infrastructure.repositories.SmtpEmailRepository;
import com.proautokimium.api.Infrastructure.repositories.email.EmailQueueRepository;
import com.proautokimium.api.Infrastructure.repositories.email.EmailRouteRepository;
import com.proautokimium.api.domain.entities.EmailEntity;
import com.proautokimium.api.domain.entities.email.EmailQueue;
import com.proautokimium.api.domain.entities.email.EmailRoute;
import com.proautokimium.api.domain.enums.EmailStatus;
import com.proautokimium.api.domain.enums.email.EmailFailureKind;
import com.proautokimium.api.domain.enums.email.EmailOrigin;
import com.proautokimium.api.domain.exceptions.humanResources.InvalidRequestDataException;
import com.proautokimium.api.domain.exceptions.humanResources.InvalidStatusTransitionException;
import com.proautokimium.api.domain.valueObjects.Email;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/** As regras das telas: reenvio, indicadores e o cadastro de remetentes e rotas. */
class EmailAdminServicesTest {

    private static final LocalDateTime AGORA = LocalDateTime.of(2026, 10, 7, 9, 0);
    private static final Clock CLOCK = Clock.fixed(AGORA.atZone(ZoneId.systemDefault()).toInstant(), ZoneId.systemDefault());

    private final EmailQueueRepository queue = mock(EmailQueueRepository.class);
    private final SmtpEmailRepository senders = mock(SmtpEmailRepository.class);
    private final EmailRouteRepository routes = mock(EmailRouteRepository.class);
    private final EmailQueueAdminService admin = new EmailQueueAdminService(queue, CLOCK);
    private final EmailSenderAdminService senderAdmin = new EmailSenderAdminService(senders, routes, queue, CLOCK);

    private static EmailQueue falhou() {
        EmailQueue e = EmailQueue.of(EmailOrigin.NEWSLETTER, "a@x.com", "s", "b", AGORA);
        e.id = UUID.randomUUID();
        e.recordImmediateFailure("550 5.1.1 User unknown", AGORA);
        return e;
    }

    private static EmailEntity sender(String name, boolean active, boolean isDefault) {
        EmailEntity e = new EmailEntity();
        e.id = UUID.randomUUID();
        e.setName(name);
        e.setEmail(new Email(name + "@envios.proautokimium.com.br"));
        e.setDisplayName(name);
        e.setActive(active);
        e.setDefault(isDefault);
        return e;
    }

    // ── fila ──

    @Test
    @DisplayName("reenviar em lote: só os que falharam voltam; o que já saiu é ignorado")
    void reenvioEmLote() {
        EmailQueue f = falhou();
        EmailQueue saiu = EmailQueue.of(EmailOrigin.NEWSLETTER, "b@x.com", "s", "b", AGORA);
        saiu.id = UUID.randomUUID();
        saiu.markSent(AGORA);
        when(queue.findAllById(any())).thenReturn(List.of(f, saiu));

        ResendResult r = admin.resend(List.of(f.getId(), saiu.getId()));

        assertThat(r.requeued()).isEqualTo(1);
        assertThat(f.getStatus()).isEqualTo(EmailStatus.PENDING);
        assertThat(saiu.getStatus()).isEqualTo(EmailStatus.SENT);
        verify(queue, never()).save(saiu);
    }

    @Test
    @DisplayName("reenviar em lote pula o e-mail com código de acesso, e a linha diz que ele não se reenvia")
    void loteIgnoraSensivel() {
        EmailQueue f = falhou();
        EmailQueue codigo = EmailQueue.of(EmailOrigin.PASSWORD_RESET, "b@x.com", "Redefinição", "<p>1</p>", AGORA);
        codigo.id = UUID.randomUUID();
        codigo.recordImmediateFailure("Connection refused", AGORA);
        when(queue.findAllById(any())).thenReturn(List.of(f, codigo));

        ResendResult r = admin.resend(List.of(f.getId(), codigo.getId()));

        assertThat(r.requeued()).isEqualTo(1);
        assertThat(codigo.getStatus()).isEqualTo(EmailStatus.FAILED);
        assertThat(EmailQueueAdminService.row(codigo, AGORA).resendable()).isFalse();
        assertThat(EmailQueueAdminService.row(falhou(), AGORA).resendable()).isTrue();
    }

    @Test
    @DisplayName("ficha de e-mail de acesso: o corpo vem escondido")
    void corpoEscondido() {
        EmailQueue codigo = EmailQueue.of(EmailOrigin.FIRST_ACCESS, "a@x.com", "Seu código", "<p>482913</p>", AGORA);
        codigo.id = UUID.randomUUID();
        when(queue.findById(codigo.getId())).thenReturn(Optional.of(codigo));

        EmailDetail d = admin.detail(codigo.getId());

        assertThat(d.bodyHidden()).isTrue();
        assertThat(d.body()).isNull();
    }

    @Test
    @DisplayName("indicadores: um dia por dia do período, inclusive os zerados, e os motivos agrupados")
    void indicadores() {
        when(queue.countByStatusSince(any())).thenReturn(List.of());
        when(queue.countByDaySince(any())).thenReturn(List.of());
        when(queue.countByOriginSince(any())).thenReturn(List.of());
        when(queue.errorsSince(any(), any())).thenReturn(List.of("550 5.1.1 User unknown", "timeout 5000", "550 no such user"));

        Summary s = admin.summary(7, null);

        assertThat(s.perDay()).hasSize(7);
        assertThat(s.perDay().get(6).date()).isEqualTo(LocalDate.of(2026, 10, 7));
        assertThat(s.perDay().get(0).date()).isEqualTo(LocalDate.of(2026, 10, 1));
        assertThat(s.reasons()).first().satisfies(r -> {
            assertThat(r.kind()).isEqualTo(EmailFailureKind.MAILBOX_NOT_FOUND);
            assertThat(r.count()).isEqualTo(2);
        });
        assertThat(s.successRate()).as("sem envios no período não há taxa, e 100% esconderia a parada").isNull();
        assertThat(s.delivery().rate()).as("nenhum rastreado: sem taxa de entrega").isNull();
    }

    @Test
    @DisplayName("período: 90 dias vale; 'desde' uma data conta até hoje e para em um ano; valor estranho vira 7")
    void periodo() {
        assertThat(admin.days(90, null)).isEqualTo(90);
        assertThat(admin.days(45, null)).isEqualTo(7);
        assertThat(admin.days(null, LocalDate.of(2026, 9, 1))).as("1/9 a 7/10, com os dois dias").isEqualTo(37);
        assertThat(admin.days(7, LocalDate.of(2026, 10, 7))).as("a data vence o atalho").isEqualTo(1);
        assertThat(admin.days(null, LocalDate.of(2026, 12, 1))).as("data futura vira hoje").isEqualTo(1);
        assertThat(admin.days(null, LocalDate.of(2020, 1, 1))).isEqualTo(EmailQueueAdminService.MAX_DAYS);
    }

    @Test
    @DisplayName("taxa de entrega: entregues ÷ rastreados concluídos (enviados + falharam), como no mockup")
    void taxaDeEntrega() {
        when(queue.countByStatusSince(any())).thenReturn(List.of());
        when(queue.countByDaySince(any())).thenReturn(List.of());
        when(queue.countByOriginSince(any())).thenReturn(List.of());
        when(queue.errorsSince(any(), any())).thenReturn(List.of());
        when(queue.countDeliverySince(any(), any())).thenReturn(new com.proautokimium.api.Infrastructure.repositories.email.EmailQueueRepository.DeliveryCount() {
            public Long getDone() { return 40L; }
            public Long getDelivered() { return 37L; }
            public Long getBounced() { return 1L; }
        });
        when(queue.countAwaitingDeliverySince(any(), any())).thenReturn(2L);

        Delivery d = admin.summary(7, null).delivery();

        assertThat(d.tracked()).isEqualTo(40);
        assertThat(d.delivered()).isEqualTo(37);
        assertThat(d.bounced()).isEqualTo(1);
        assertThat(d.awaiting()).isEqualTo(2);
        assertThat(d.rate()).isEqualTo(92.5);
    }

    // ── remetentes ──

    @Test
    @DisplayName("novo remetente: nome inválido, repetido ou sem nome de exibição é recusado")
    void novoRemetenteValida() {
        assertThrows(InvalidRequestDataException.class, () -> senderAdmin.create(new CreateSender("Fin Anceiro", "Financeiro")));
        assertThrows(InvalidRequestDataException.class, () -> senderAdmin.create(new CreateSender("financeiro", " ")));
        when(senders.existsByEmail_AddressIgnoreCase("rh@envios.proautokimium.com.br")).thenReturn(true);
        assertThrows(InvalidRequestDataException.class, () -> senderAdmin.create(new CreateSender("rh", "RH")));
        verify(senders, never()).save(any());
    }

    @Test
    @DisplayName("desativar: em uso por uma rota ou sendo o padrão, recusado")
    void desativarEmUso() {
        EmailEntity rh = sender("rh", true, false);
        EmailEntity padrao = sender("noreply", true, true);
        when(senders.findById(rh.getId())).thenReturn(Optional.of(rh));
        when(senders.findById(padrao.getId())).thenReturn(Optional.of(padrao));
        when(routes.existsBySender_IdOrReplyTo_Id(rh.getId(), rh.getId())).thenReturn(true);

        assertThrows(InvalidStatusTransitionException.class, () -> senderAdmin.update(rh.getId(), new UpdateSender(null, false)));
        assertThrows(InvalidStatusTransitionException.class, () -> senderAdmin.update(padrao.getId(), new UpdateSender(null, false)));
        assertThat(rh.isActive()).isTrue();
        assertThat(padrao.isActive()).isTrue();
    }

    @Test
    @DisplayName("rota para remetente inativo é recusada; rota sem remetente volta para o padrão")
    void rotas() {
        EmailEntity inativo = sender("velho", false, false);
        when(senders.findById(inativo.getId())).thenReturn(Optional.of(inativo));
        assertThrows(InvalidRequestDataException.class,
                () -> senderAdmin.updateRoute(EmailOrigin.CHECKLIST, new UpdateRoute(inativo.getId(), null), "dev"));

        EmailRoute atual = new EmailRoute(EmailOrigin.CHECKLIST, sender("rh", true, false), null, "dev", AGORA);
        when(routes.findById(EmailOrigin.CHECKLIST)).thenReturn(Optional.of(atual));
        when(queue.countForRoutes(any())).thenReturn(List.of());
        senderAdmin.updateRoute(EmailOrigin.CHECKLIST, new UpdateRoute(null, null), "dev");
        verify(routes).delete(atual);
    }

    @Test
    @DisplayName("envio manual não tem rota: o remetente é escolhido a cada envio, e um seletor ali não faria nada")
    void manualSemRota() {
        assertThat(senderAdmin.listRoutes()).extracting(Route::origin)
                .doesNotContain(EmailOrigin.MANUAL)
                .contains(EmailOrigin.NEWSLETTER, EmailOrigin.FIRST_ACCESS);

        assertThrows(InvalidRequestDataException.class,
                () -> senderAdmin.updateRoute(EmailOrigin.MANUAL, new UpdateRoute(null, null), "dev"));
    }
}
