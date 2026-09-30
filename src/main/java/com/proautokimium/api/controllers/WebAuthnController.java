package com.proautokimium.api.controllers;

import com.proautokimium.api.Application.DTOs.user.LoginResponseDTO;
import com.proautokimium.api.Application.DTOs.webauthn.AuthenticateCredentialDTO;
import com.proautokimium.api.Application.DTOs.webauthn.AuthenticationOptionsDTO;
import com.proautokimium.api.Application.DTOs.webauthn.RegisterCredentialDTO;
import com.proautokimium.api.Application.DTOs.webauthn.RegistrationOptionsDTO;
import com.proautokimium.api.Application.DTOs.webauthn.WebAuthnCredentialDTO;
import com.proautokimium.api.Infrastructure.services.authentication.webauthn.WebAuthnService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

/**
 * Entrar com a digital.
 *
 * <p>Cadastro, lista e remoção são da própria pessoa logada — sem tela de
 * permissão, como o Perfil. As duas rotas de login são públicas
 * ({@code SecurityPaths.PUBLIC_POST}): quem as chama ainda não entrou.
 */
@RestController
@RequestMapping("api/auth/webauthn")
@Tag(name = "Login com digital", description = "WebAuthn / passkeys")
public class WebAuthnController {

    private final WebAuthnService service;

    public WebAuthnController(WebAuthnService service) {
        this.service = service;
    }

    @PostMapping("/registration/options")
    @Operation(summary = "Começa a ativar a digital neste aparelho")
    public ResponseEntity<RegistrationOptionsDTO> registrationOptions(Authentication auth) {
        return ResponseEntity.ok(service.registrationOptions(auth.getName()));
    }

    @PostMapping("/registration")
    @Operation(summary = "Conclui a ativação: grava a chave pública do aparelho")
    public ResponseEntity<WebAuthnCredentialDTO> register(@RequestBody @Valid RegisterCredentialDTO dto,
                                                          @RequestHeader(value = HttpHeaders.USER_AGENT, required = false) String userAgent,
                                                          Authentication auth) {
        return ResponseEntity.ok(service.register(auth.getName(), dto, userAgent));
    }

    @PostMapping("/authentication/options")
    @Operation(summary = "Começa o login com a digital", description = "Público: devolve só o desafio, sem dizer quem tem digital")
    public ResponseEntity<AuthenticationOptionsDTO> authenticationOptions() {
        return ResponseEntity.ok(service.authenticationOptions());
    }

    @PostMapping("/authentication")
    @Operation(summary = "Conclui o login com a digital", description = "Público: confere a assinatura e devolve os tokens")
    public ResponseEntity<LoginResponseDTO> authenticate(@RequestBody @Valid AuthenticateCredentialDTO dto) {
        return ResponseEntity.ok(service.login(dto));
    }

    @GetMapping("/credentials")
    @Operation(summary = "Os aparelhos com digital da pessoa logada")
    public ResponseEntity<List<WebAuthnCredentialDTO>> mine(Authentication auth) {
        return ResponseEntity.ok(service.listMine(auth.getName()));
    }

    @DeleteMapping("/credentials/{id}")
    @Operation(summary = "Remove um aparelho da pessoa logada")
    public ResponseEntity<Void> removeMine(@PathVariable UUID id, Authentication auth) {
        service.removeMine(auth.getName(), id);
        return ResponseEntity.noContent().build();
    }
}
