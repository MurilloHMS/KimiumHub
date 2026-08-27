package com.proautokimium.api.controllers;

import com.proautokimium.api.Infrastructure.repositories.UserRepository;
import com.proautokimium.api.Infrastructure.services.permission.PermissionService;
import com.proautokimium.api.domain.entities.auth.User;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * O que é do próprio usuário logado.
 *
 * Controller novo em vez de mais um `/me` espalhado: as permissões não são de
 * reembolso nem de holerite, e pendurá-las num controller existente faria a
 * tela de login depender de um módulo que não tem nada a ver.
 */
@RestController
@RequestMapping("api/me")
public class MeController {

    private final UserRepository userRepository;
    private final PermissionService permissionService;

    public MeController(UserRepository userRepository, PermissionService permissionService) {
        this.userRepository = userRepository;
        this.permissionService = permissionService;
    }

    /**
     * As permissões da pessoa, agrupadas por tela.
     *
     * **Sem `@PreAuthorize`, de propósito.** Todo mundo pode ler as próprias
     * permissões — exigir permissão para saber quais permissões você tem seria
     * um nó que ninguém desata. O que protege é o `auth`: só devolve as de quem
     * está pedindo, nunca as de outro.
     */
    @GetMapping("/permissions")
    public ResponseEntity<?> myPermissions(Authentication auth) {
        User user = (User) userRepository.findByLogin(auth.getName());
        return ResponseEntity.ok(permissionService.permissionsByScreen(user.getId()));
    }
}
