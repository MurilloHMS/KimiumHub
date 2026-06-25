package com.proautokimium.api.Infrastructure.services.push;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.proautokimium.api.Infrastructure.repositories.PushSubscriptionRepository;
import com.proautokimium.api.domain.entities.PushSubscriptionEntity;
import jakarta.annotation.PostConstruct;
import nl.martijndwars.webpush.Notification;
import nl.martijndwars.webpush.PushService;
import nl.martijndwars.webpush.Subscription;
import org.apache.http.HttpResponse;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.Security;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Envia notificações Web Push (VAPID) usando as inscrições salvas.
 * O payload segue o formato esperado pelo service worker do Angular ({ "notification": {...} }),
 * então a notificação é exibida automaticamente, mesmo com o site fechado.
 */
@Service
public class WebPushService {

    private static final Logger log = LoggerFactory.getLogger(WebPushService.class);

    private final PushSubscriptionRepository repository;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Value("${push.vapid.public.key:}")
    private String publicKey;

    @Value("${push.vapid.private.key:}")
    private String privateKey;

    @Value("${push.vapid.subject:mailto:contato@proautokimium.com.br}")
    private String subject;

    private PushService pushService;
    private boolean enabled = false;

    public WebPushService(PushSubscriptionRepository repository) {
        this.repository = repository;
    }

    @PostConstruct
    void init() {
        if (publicKey == null || publicKey.isBlank() || privateKey == null || privateKey.isBlank()) {
            log.warn("Web Push desabilitado: chaves VAPID não configuradas (push.vapid.public.key / push.vapid.private.key).");
            return;
        }
        try {
            if (Security.getProvider(BouncyCastleProvider.PROVIDER_NAME) == null) {
                Security.addProvider(new BouncyCastleProvider());
            }
            this.pushService = new PushService(publicKey, privateKey, subject);
            this.enabled = true;
            log.info("Web Push habilitado (VAPID).");
        } catch (Exception e) {
            log.error("Falha ao inicializar o Web Push: {}", e.getMessage());
        }
    }

    public String getPublicKey() {
        return publicKey;
    }

    public boolean isEnabled() {
        return enabled;
    }

    @Transactional
    public void subscribe(String login, String endpoint, String p256dh, String auth) {
        PushSubscriptionEntity entity = repository.findByEndpoint(endpoint).orElse(null);
        if (entity == null) {
            entity = new PushSubscriptionEntity(login, endpoint, p256dh, auth);
        } else {
            entity.setRecipientLogin(login);
            entity.setP256dh(p256dh);
            entity.setAuth(auth);
        }
        repository.save(entity);
    }

    @Transactional
    public void unsubscribe(String endpoint) {
        repository.deleteByEndpoint(endpoint);
    }

    /** Envia o push para todos os dispositivos inscritos do usuário. Best-effort. */
    @Transactional
    public void sendToUser(String login, String title, String body, String link) {
        if (!enabled) return;

        List<PushSubscriptionEntity> subs = repository.findByRecipientLogin(login);
        if (subs.isEmpty()) return;

        final String payload = buildPayload(title, body, link);

        for (PushSubscriptionEntity sub : subs) {
            try {
                Subscription subscription = new Subscription(
                        sub.getEndpoint(), new Subscription.Keys(sub.getP256dh(), sub.getAuth()));
                HttpResponse response = pushService.send(new Notification(subscription, payload));
                int status = response.getStatusLine().getStatusCode();
                // 404/410 = inscrição expirada/cancelada → remove
                if (status == 404 || status == 410) {
                    repository.deleteByEndpoint(sub.getEndpoint());
                }
            } catch (Exception e) {
                log.warn("Falha ao enviar push para {}: {}", login, e.getMessage());
            }
        }
    }

    private String buildPayload(String title, String body, String link) {
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("url", link == null || link.isBlank() ? "/" : link);

        Map<String, Object> notification = new LinkedHashMap<>();
        notification.put("title", title);
        notification.put("body", body);
        notification.put("icon", "/icons/icon-192x192.png");
        notification.put("badge", "/icons/badge-72x72.png");
        notification.put("vibrate", List.of(100, 50, 100));
        notification.put("data", data);

        try {
            return objectMapper.writeValueAsString(Map.of("notification", notification));
        } catch (Exception e) {
            // fallback simples
            return "{\"notification\":{\"title\":\"" + title + "\",\"body\":\"" + body + "\"}}";
        }
    }
}
