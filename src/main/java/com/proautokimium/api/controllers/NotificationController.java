package com.proautokimium.api.controllers;

import com.proautokimium.api.Application.DTOs.notification.NotificationDTO;
import com.proautokimium.api.Infrastructure.services.notification.NotificationService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("api/notifications")
public class NotificationController {

    private final NotificationService service;

    public NotificationController(NotificationService service) {
        this.service = service;
    }

    /** Lista as notificações do usuário logado (mais recentes primeiro). */
    @GetMapping
    public ResponseEntity<List<NotificationDTO>> listar(Authentication auth) {
        return ResponseEntity.ok(service.listar(auth.getName()));
    }

    /** Quantidade de notificações não lidas (para o badge). */
    @GetMapping("/unread-count")
    public ResponseEntity<Map<String, Long>> naoLidas(Authentication auth) {
        return ResponseEntity.ok(Map.of("count", service.contarNaoLidas(auth.getName())));
    }

    @PutMapping("/{id}/read")
    public ResponseEntity<Void> marcarLida(@PathVariable UUID id, Authentication auth) {
        return service.marcarComoLida(id, auth.getName())
                ? ResponseEntity.ok().build()
                : ResponseEntity.notFound().build();
    }

    @PutMapping("/read-all")
    public ResponseEntity<Map<String, Integer>> marcarTodasLidas(Authentication auth) {
        return ResponseEntity.ok(Map.of("updated", service.marcarTodasComoLidas(auth.getName())));
    }
}
