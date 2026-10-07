package com.proautokimium.api.Infrastructure.services.email;

import com.proautokimium.api.Infrastructure.repositories.SmtpEmailRepository;
import com.proautokimium.api.Infrastructure.repositories.email.EmailRouteRepository;
import com.proautokimium.api.domain.entities.EmailEntity;
import com.proautokimium.api.domain.entities.email.EmailRoute;
import com.proautokimium.api.domain.enums.email.EmailOrigin;
import com.proautokimium.api.domain.valueObjects.Email;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/** De qual e-mail sai cada origem: rota ativa → padrão → EMAIL_FROM. */
class EmailSenderResolverTest {

    private final EmailRouteRepository routes = mock(EmailRouteRepository.class);
    private final SmtpEmailRepository senders = mock(SmtpEmailRepository.class);
    private final EmailSenderResolver resolver = new EmailSenderResolver(routes, senders, "env@proautokimium.com.br");

    private static EmailEntity sender(String name, String display, boolean active) {
        EmailEntity e = new EmailEntity();
        e.id = UUID.randomUUID();
        e.setName(name);
        e.setEmail(new Email(name + "@envios.proautokimium.com.br"));
        e.setDisplayName(display);
        e.setActive(active);
        return e;
    }

    @Test
    @DisplayName("com rota: sai do remetente dela, com o nome de exibição e o responder-para")
    void rota() {
        EmailEntity rh = sender("rh", "RH Proauto", true);
        when(routes.findById(EmailOrigin.DOCUMENT_ALERT)).thenReturn(Optional.of(
                new EmailRoute(EmailOrigin.DOCUMENT_ALERT, rh, rh, "dev", LocalDateTime.now())));

        EmailSenderResolver.Sender s = resolver.resolve(EmailOrigin.DOCUMENT_ALERT);

        assertThat(s).isEqualTo(new EmailSenderResolver.Sender("rh@envios.proautokimium.com.br", "RH Proauto",
                "rh@envios.proautokimium.com.br"));
    }

    @Test
    @DisplayName("rota com remetente desativado: cai no padrão")
    void rotaInativaCaiNoPadrao() {
        when(routes.findById(EmailOrigin.CHECKLIST)).thenReturn(Optional.of(
                new EmailRoute(EmailOrigin.CHECKLIST, sender("velho", "Velho", false), null, "dev", LocalDateTime.now())));
        when(senders.findFirstByIsDefaultTrue()).thenReturn(Optional.of(sender("noreply", null, true)));

        EmailSenderResolver.Sender s = resolver.resolve(EmailOrigin.CHECKLIST);

        assertThat(s.address()).isEqualTo("noreply@envios.proautokimium.com.br");
        assertThat(s.name()).as("sem nome de exibição, o de sempre").isEqualTo("Proauto Kimium");
    }

    @Test
    @DisplayName("sem rota e sem padrão: o EMAIL_FROM do ambiente, como era antes")
    void semNadaUsaOAmbiente() {
        when(routes.findById(EmailOrigin.RECRUITMENT)).thenReturn(Optional.empty());
        when(senders.findFirstByIsDefaultTrue()).thenReturn(Optional.empty());

        assertThat(resolver.resolve(EmailOrigin.RECRUITMENT).address()).isEqualTo("env@proautokimium.com.br");
    }
}
