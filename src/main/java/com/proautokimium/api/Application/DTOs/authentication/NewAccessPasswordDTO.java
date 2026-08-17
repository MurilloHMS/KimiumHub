package com.proautokimium.api.Application.DTOs.authentication;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

/**
 * A senha é conferida aqui, e não só na tela. O endpoint é público: o front
 * recusa a senha fraca e um curl escolhe o que quiser.
 */
public record NewAccessPasswordDTO(
        @NotBlank
        @Pattern(
                regexp = "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[@$!%*?&#])[A-Za-z\\d@$!%*?&#]{8,}$",
                message = "A senha precisa ter 8 caracteres, com maiúscula, minúscula, número e símbolo (@ $ ! % * ? & #)."
        )
        String password,

        @Email
        String email
) { }