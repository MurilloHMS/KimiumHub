package com.proautokimium.api.controllers.humanResources;

import com.proautokimium.api.Infrastructure.services.humanResources.EmployeeDocumentAlertService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * "Rodar os avisos agora" — o mesmo que o agendamento das 8h faz.
 *
 * É seguro apertar de novo: cada marco sai uma vez só (tabela de enviados),
 * então rodar duas vezes no mesmo dia não repete aviso. Serve para conferir a
 * configuração de um tipo sem esperar o dia seguinte.
 *
 * Controller próprio, e não um endpoint no de documentos: aquele entra no teste
 * de download (`FileDownloadAccessTest`), que teria de simular este serviço
 * também sem ter nada a ver com ele.
 */
@RestController
@RequestMapping("/api/hr/employee-document-alerts")
@Tag(name = "Avisos de vencimento de documento")
public class EmployeeDocumentAlertController {

    private final EmployeeDocumentAlertService service;

    public EmployeeDocumentAlertController(EmployeeDocumentAlertService service) {
        this.service = service;
    }

    @PostMapping("/run")
    @PreAuthorize("hasAuthority('rh/employee-documents:CONFIGURAR')")
    @Operation(summary = "Roda os avisos de hoje agora", description = "Devolve quantos documentos geraram aviso")
    public ResponseEntity<Map<String, Integer>> run(){
        return ResponseEntity.ok(Map.of("alerted", service.runAlerts()));
    }
}
