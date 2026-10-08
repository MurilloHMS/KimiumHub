package com.proautokimium.api.Application.DTOs.user;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

/**
 * O que a tela de administração edita numa conta.
 *
 * Só o e-mail. O login fica de fora de propósito: é o sujeito do JWT e a chave
 * do `SecurityFilter`, e trocá-lo derrubaria a sessão da pessoa no meio do
 * trabalho. O vínculo com o funcionário tem endpoints próprios.
 */
public record UpdateUserRequest(
        @NotBlank(message = "Informe o e-mail.")
        @Email(message = "E-mail inválido.")
        String email) {
}
