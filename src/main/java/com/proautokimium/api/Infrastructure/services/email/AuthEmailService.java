package com.proautokimium.api.Infrastructure.services.email;

import com.proautokimium.api.Infrastructure.services.authentication.TokenAuthService;
import com.proautokimium.api.domain.entities.auth.User;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Map;

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

    private final EmailRenderer renderer;
    private final EmailQueueService emailQueueService;
    private final String websiteBaseUrl;

    public AuthEmailService(EmailRenderer renderer,
                            EmailQueueService emailQueueService,
                            @Value("${app.base-url}") String websiteBaseUrl) {

        this.renderer = renderer;
        this.emailQueueService = emailQueueService;
        this.websiteBaseUrl = websiteBaseUrl;
    }

    public void sendFirstAccessToken(String to, String token) {
        String html = codeEmail(FIRST_ACCESS_TEMPLATE, to, token, "/login/first-access");
        emailQueueService.sendNow(to, FROM, "Seu código de primeiro acesso", html);
    }

    public void sendResetPasswordToken(User user, String token){
        String deepUrl = user.getCustomer() != null
                ? "/cliente/redefinir-senha"
                : "/login/forgot-password";

        String html = codeEmail(RESET_ACCESS_TEMPLATE, user.getEmail(), token, deepUrl);
        emailQueueService.sendNow(user.getEmail(), FROM, "Seu código de redefinição de senha", html);
    }

    private String buildDeepUrlWithToken(String email, String token, String url){
        return websiteBaseUrl + url
                + "?token=" + URLEncoder.encode(token, StandardCharsets.UTF_8)
                + "&email=" + URLEncoder.encode(email, StandardCharsets.UTF_8);
    }

    /** Os dois e-mails de código (primeiro acesso e redefinição) têm as mesmas variáveis. */
    private String codeEmail(String template, String to, String token, String deepUrl){
        return renderer.render(template, Map.of(
                "token", token,
                "ttlMinutes", TokenAuthService.TOKEN_TTL_MINUTES,
                "actionUrl", buildDeepUrlWithToken(to, token, deepUrl)
        ));
    }

    /**
     * Convite do portal. Diferente do primeiro acesso do funcionário em tudo
     * que importa: o cliente não pediu, não digita código nenhum, e o link é a
     * única coisa que ele precisa guardar — por isso 48 horas e não 30 minutos.
     */
    public void sendClientInvite(String to, String token, String customerName) {
        // O convite tem as variáveis dele: o nome da empresa e o prazo em horas.
        String html = renderer.render(CLIENT_INVITE_TEMPLATE, Map.of(
                "customerName", customerName,
                "ttlHours", TokenAuthService.INVITE_TTL_HOURS,
                "actionUrl", buildDeepUrlWithToken(to, token, "/cliente/primeiro-acesso")
        ));
        emailQueueService.sendNow(to, FROM, "Seu acesso ao Portal Proauto Kimium", html);
    }
}
