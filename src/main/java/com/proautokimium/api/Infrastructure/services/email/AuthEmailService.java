package com.proautokimium.api.Infrastructure.services.email;

import com.proautokimium.api.Infrastructure.services.authentication.TokenAuthService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.stereotype.Service;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

/**
 * Composição dos e-mails transacionais de autenticação (templates Thymeleaf).
 * O envio em si continua com o EmailQueueService (sendNow).
 */
@Service
public class AuthEmailService {

    private static final String FROM = "noreply@envios.proautokimium.com.br";
    private static final String FIRST_ACCESS_TEMPLATE = "html/first-access-token";

    private final TemplateEngine templateEngine;
    private final EmailQueueService emailQueueService;
    private final String websiteBaseUrl;

    public AuthEmailService(TemplateEngine templateEngine,
                            EmailQueueService emailQueueService,
                            @Value("${app.base-url}") String websiteBaseUrl) {
        this.templateEngine = templateEngine;
        this.emailQueueService = emailQueueService;
        this.websiteBaseUrl = websiteBaseUrl;
    }

    public void sendFirstAccessToken(String to, String token) {
        Context ctx = new Context(LocaleContextHolder.getLocale());
        ctx.setVariable("token", token);
        ctx.setVariable("ttlMinutes", TokenAuthService.TOKEN_TTL_MINUTES);
        ctx.setVariable("actionUrl", buildFirstAccessUrl(to, token));

        String html = templateEngine.process(FIRST_ACCESS_TEMPLATE, ctx);
        emailQueueService.sendNow(to, FROM, "Seu código de primeiro acesso", html);
    }

    /** Deep link com token e e-mail: o front pula direto para a validação do código. */
    private String buildFirstAccessUrl(String email, String token) {
        return websiteBaseUrl + "/login/first-access"
                + "?token=" + URLEncoder.encode(token, StandardCharsets.UTF_8)
                + "&email=" + URLEncoder.encode(email, StandardCharsets.UTF_8);
    }
}
