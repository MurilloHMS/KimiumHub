package com.proautokimium.api.Application.DTOs.webauthn;

import java.util.UUID;

/**
 * O que o navegador precisa para {@code navigator.credentials.get()}. Sem lista
 * de credenciais de propósito: quem sabe qual usar é o aparelho, que guardou o
 * id no cadastro. O servidor não diz se um login tem digital.
 */
public record AuthenticationOptionsDTO(UUID challengeId, String challenge, String rpId, long timeoutMs) {}
