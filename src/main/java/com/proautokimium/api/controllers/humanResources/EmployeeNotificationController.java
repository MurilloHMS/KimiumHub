package com.proautokimium.api.controllers.humanResources;

import com.proautokimium.api.Application.DTOs.humanResources.Notification.SendNotificationRequestDTO;
import com.proautokimium.api.Application.DTOs.humanResources.Notification.SendNotificationResponseDTO;
import com.proautokimium.api.Infrastructure.services.humanResources.EmployeeNotificationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/hr/notifications")
@Tag(name = "Notificações do RH", description = "RH envia mensagem personalizada a um, vários ou todos os funcionários")
public class EmployeeNotificationController {

    private final EmployeeNotificationService service;

    public EmployeeNotificationController(EmployeeNotificationService service) {
        this.service = service;
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'RH')")
    @Operation(summary = "Envia notificação", description = "employeeIds vazio/nulo envia para todos os funcionários ativos")
    public ResponseEntity<SendNotificationResponseDTO> send(@Valid @RequestBody SendNotificationRequestDTO request) {
        return ResponseEntity.ok(service.send(request));
    }
}
