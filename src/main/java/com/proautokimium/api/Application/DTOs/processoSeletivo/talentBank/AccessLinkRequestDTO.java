package com.proautokimium.api.Application.DTOs.processoSeletivo.talentBank;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

/**
 * "Quero ver o que enviei."
 *
 * <p>O 400 aqui é só sobre o <b>formato</b> do campo. Se o endereço existe ou
 * não na base, a resposta é a mesma — 202 — e essa diferença é a linha entre
 * um formulário e um oráculo de enumeração.
 */
public record AccessLinkRequestDTO(
        @NotBlank(message = "Informe seu e-mail.")
        @Email(message = "E-mail inválido.")
        String email
) { }
