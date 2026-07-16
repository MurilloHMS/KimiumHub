package com.proautokimium.api.Infrastructure.services.email;

import com.proautokimium.api.Infrastructure.services.authentication.TokenAuthService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.IContext;
import org.thymeleaf.context.Context;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

class AuthEmailServiceTest {

    private final TemplateEngine templateEngine = mock(TemplateEngine.class);
    private final EmailQueueService emailQueueService = mock(EmailQueueService.class);

    private final AuthEmailService service =
            new AuthEmailService(templateEngine, emailQueueService, "https://site.teste");

    @Test
    @DisplayName("Deve renderizar o template com token, TTL e deep link e enviar imediatamente")
    void shouldRenderTemplateAndSendImmediately() {
        when(templateEngine.process(eq("html/first-access-token"), any(IContext.class)))
                .thenReturn("<html>renderizado</html>");

        service.sendFirstAccessToken("novo@teste.com", "ABC123");

        ArgumentCaptor<IContext> ctxCaptor = ArgumentCaptor.forClass(IContext.class);
        verify(templateEngine).process(eq("html/first-access-token"), ctxCaptor.capture());
        Context ctx = (Context) ctxCaptor.getValue();

        assertThat(ctx.getVariable("token")).isEqualTo("ABC123");
        assertThat(ctx.getVariable("ttlMinutes")).isEqualTo(TokenAuthService.TOKEN_TTL_MINUTES);
        assertThat(ctx.getVariable("actionUrl"))
                .isEqualTo("https://site.teste/login/first-access?token=ABC123&email=novo%40teste.com");

        verify(emailQueueService).sendNow(
                eq("novo@teste.com"),
                eq("noreply@envios.proautokimium.com.br"),
                eq("Seu código de primeiro acesso"),
                eq("<html>renderizado</html>"));
    }

    @Test
    @DisplayName("Falha no envio deve propagar (sem falso sucesso para quem pediu o código)")
    void shouldPropagateFailureWhenSendFails() {
        when(templateEngine.process(eq("html/first-access-token"), any(IContext.class)))
                .thenReturn("<html>renderizado</html>");
        doThrow(new RuntimeException("SMTP fora do ar"))
                .when(emailQueueService).sendNow(any(), any(), any(), any());

        org.assertj.core.api.Assertions.assertThatThrownBy(
                        () -> service.sendFirstAccessToken("novo@teste.com", "ABC123"))
                .isInstanceOf(RuntimeException.class);
    }
}
