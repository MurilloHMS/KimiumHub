package com.proautokimium.api.controllers;

import com.proautokimium.api.Application.DTOs.push.PushSubscriptionRequest;
import com.proautokimium.api.Infrastructure.services.push.WebPushService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("api/push")
public class PushController {

    private final WebPushService webPushService;

    public PushController(WebPushService webPushService) {
        this.webPushService = webPushService;
    }

    /** Chave pública VAPID usada pelo navegador para se inscrever. */
    @GetMapping("/public-key")
    public ResponseEntity<Map<String, String>> publicKey() {
        return ResponseEntity.ok(Map.of("publicKey", webPushService.getPublicKey()));
    }

    /** Registra a inscrição de push do dispositivo do usuário logado. */
    @PostMapping("/subscribe")
    public ResponseEntity<Void> subscribe(@RequestBody PushSubscriptionRequest req, Authentication auth) {
        if (req == null || req.endpoint() == null || req.keys() == null) {
            return ResponseEntity.badRequest().build();
        }
        webPushService.subscribe(auth.getName(), req.endpoint(), req.keys().p256dh(), req.keys().auth());
        return ResponseEntity.ok().build();
    }

    /** Remove a inscrição (logout / desativação do push no dispositivo). */
    @PostMapping("/unsubscribe")
    public ResponseEntity<Void> unsubscribe(@RequestBody Map<String, String> body) {
        String endpoint = body.get("endpoint");
        if (endpoint != null && !endpoint.isBlank()) {
            webPushService.unsubscribe(endpoint);
        }
        return ResponseEntity.ok().build();
    }
}
