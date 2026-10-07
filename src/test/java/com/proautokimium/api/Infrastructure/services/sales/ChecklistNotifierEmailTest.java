package com.proautokimium.api.Infrastructure.services.sales;

import com.proautokimium.api.Infrastructure.repositories.UserRepository;
import com.proautokimium.api.Infrastructure.services.email.EmailQueueService;
import com.proautokimium.api.Infrastructure.services.email.EmailRenderer;
import com.proautokimium.api.Infrastructure.services.notification.NotificationService;
import com.proautokimium.api.domain.entities.auth.User;
import com.proautokimium.api.domain.entities.sales.Checklist;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.thymeleaf.spring6.SpringTemplateEngine;
import org.thymeleaf.templateresolver.ClassLoaderTemplateResolver;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * O e-mail que a Controladoria recebe, com o template de verdade: o texto do
 * vendedor chega escapado (antes era o HtmlUtils, agora é o th:text) e o botão
 * leva ao endereço completo do site, porque e-mail não entende link relativo.
 */
class ChecklistNotifierEmailTest {

    private final UserRepository users = mock(UserRepository.class);
    private final EmailQueueService fila = mock(EmailQueueService.class);
    private ChecklistNotifier notifier;

    @BeforeEach
    void setUp() {
        ClassLoaderTemplateResolver resolver = new ClassLoaderTemplateResolver();
        resolver.setPrefix("templates/");
        resolver.setSuffix(".html");
        resolver.setTemplateMode("HTML");
        resolver.setCharacterEncoding("UTF-8");
        SpringTemplateEngine engine = new SpringTemplateEngine();
        engine.setTemplateResolver(resolver);

        notifier = new ChecklistNotifier(users, mock(NotificationService.class), fila,
                "https://www.proautokimium.com.br", new EmailRenderer(engine));

        User controladoria = new User();
        controladoria.setLogin("rita");
        controladoria.setEmail("rita@proautokimium.com.br");
        controladoria.setActive(true);
        when(users.findByRolesIn(any())).thenReturn(List.of(controladoria));
    }

    private String emailEnviado() {
        ArgumentCaptor<String> corpo = ArgumentCaptor.forClass(String.class);
        verify(fila).sendEmail(eq("rita@proautokimium.com.br"), anyString(), anyString(), corpo.capture());
        return corpo.getValue();
    }

    @Test
    @DisplayName("checklist enviado: título, mensagem e botão com o endereço completo do site")
    void enviado() {
        Checklist c = mock(Checklist.class);
        when(c.getVersion()).thenReturn(1);
        when(c.getNumber()).thenReturn(42L);
        when(c.getSellerName()).thenReturn("Rafael Gomes");
        when(c.getCustomerName()).thenReturn("Lavanderia Central");

        notifier.submitted(c);

        String html = emailEnviado();
        assertThat(html).contains(">Novo checklist para conferir</h1>")
                .contains("Rafael Gomes enviou o checklist nº 0042 — Lavanderia Central.")
                .contains("href=\"https://www.proautokimium.com.br/vendas/checklists\"");
    }

    @Test
    @DisplayName("o texto que o vendedor digitou chega escapado, nunca como HTML")
    void textoEscapado() {
        Checklist c = mock(Checklist.class);
        when(c.getVersion()).thenReturn(1);
        when(c.getNumber()).thenReturn(7L);
        when(c.getSellerName()).thenReturn("Rafael");
        when(c.getCustomerName()).thenReturn("<script>alert(1)</script>");

        notifier.submitted(c);

        assertThat(emailEnviado()).doesNotContain("<script>").contains("&lt;script&gt;");
    }
}
