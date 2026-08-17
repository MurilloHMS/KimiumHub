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
    private static final String RESET_ACCESS_TEMPLATE = "html/reset-access-token";
    private static final String CLIENT_INVITE_TEMPLATE = "html/client-invite";

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
        Context ctx = createContext(to, token, "/login/first-access");
        String html = templateEngine.process(FIRST_ACCESS_TEMPLATE, ctx);
        emailQueueService.sendNow(to, FROM, "Seu código de primeiro acesso", html);
    }

    public void sendResetPasswordToken(String to, String token){
        Context ctx = createContext(to,token, "/login/forgot-password");
        String html = templateEngine.process(RESET_ACCESS_TEMPLATE, ctx);
        emailQueueService.sendNow(to, FROM, "Seu código de redefinição de senha", html);
    }

    private String buildDeepUrlWithToken(String email, String token, String url){
        return websiteBaseUrl + url
                + "?token=" + URLEncoder.encode(token, StandardCharsets.UTF_8)
                + "&email=" + URLEncoder.encode(email, StandardCharsets.UTF_8);
    }

    private Context createContext(String to, String token, String deepUrl){
        Context ctx = new Context(LocaleContextHolder.getLocale());
        ctx.setVariable("token", token);
        ctx.setVariable("ttlMinutes", TokenAuthService.TOKEN_TTL_MINUTES);
        ctx.setVariable("actionUrl", buildDeepUrlWithToken(to, token, deepUrl));
        return ctx;
    }

    /**
     * Convite do portal. Diferente do primeiro acesso do funcionário em tudo
     * que importa: o cliente não pediu, não digita código nenhum, e o link é a
     * única coisa que ele precisa guardar — por isso 48 horas e não 30 minutos.
     */
    public void sendClientInvite(String to, String token, String customerName) {
        Context ctx = new Context(LocaleContextHolder.getLocale());
        ctx.setVariable("customerName", customerName);
        ctx.setVariable("ttlHours", TokenAuthService.INVITE_TTL_HOURS);
        ctx.setVariable("actionUrl", buildDeepUrlWithToken(to, token, "/cliente/primeiro-acesso"));

        String html = templateEngine.process(CLIENT_INVITE_TEMPLATE, ctx);
        emailQueueService.sendNow(to, FROM, "Seu acesso ao Portal Proauto Kimium", html);
    }
}
