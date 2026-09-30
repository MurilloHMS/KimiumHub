package com.proautokimium.api.controllers;

import com.proautokimium.api.Application.DTOs.webauthn.WebAuthnCredentialDTO;
import com.proautokimium.api.Infrastructure.services.authentication.webauthn.WebAuthnService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

/**
 * A seção "Acesso com digital" do cadastro do funcionário: o RH e o ADMIN
 * veem os aparelhos e removem (decisão dele, 2026-09-30). Mesmas permissões da
 * tela: ver é CONSULTAR, remover é ALTERAR.
 */
@RestController
@RequestMapping("api/employee/{employeeId}/webauthn-credentials")
@Tag(name = "Funcionários", description = "Aparelhos com digital do funcionário")
public class EmployeeWebAuthnController {

    private final WebAuthnService service;

    public EmployeeWebAuthnController(WebAuthnService service) {
        this.service = service;
    }

    @PreAuthorize("hasAuthority('rh/employees:CONSULTAR')")
    @GetMapping
    @Operation(summary = "Aparelhos com digital do funcionário")
    public ResponseEntity<List<WebAuthnCredentialDTO>> list(@PathVariable UUID employeeId) {
        return ResponseEntity.ok(service.listForEmployee(employeeId));
    }

    @PreAuthorize("hasAuthority('rh/employees:ALTERAR')")
    @DeleteMapping("/{credentialId}")
    @Operation(summary = "Remove um aparelho do funcionário")
    public ResponseEntity<Void> remove(@PathVariable UUID employeeId, @PathVariable UUID credentialId) {
        service.removeForEmployee(employeeId, credentialId);
        return ResponseEntity.noContent().build();
    }

    @PreAuthorize("hasAuthority('rh/employees:ALTERAR')")
    @DeleteMapping
    @Operation(summary = "Remove todos os aparelhos do funcionário")
    public ResponseEntity<Void> removeAll(@PathVariable UUID employeeId) {
        service.removeForEmployee(employeeId, null);
        return ResponseEntity.noContent().build();
    }
}
