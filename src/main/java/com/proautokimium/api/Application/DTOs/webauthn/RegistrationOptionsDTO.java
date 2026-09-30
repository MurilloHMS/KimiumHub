package com.proautokimium.api.Application.DTOs.webauthn;

import java.util.List;
import java.util.UUID;

/**
 * O que o navegador precisa para {@code navigator.credentials.create()}. Os
 * binários vão em base64url; o site converte para {@code ArrayBuffer}.
 *
 * @param challengeId volta no cadastro, para o servidor achar o desafio
 * @param userId      o "user handle": o id interno, nunca login ou e-mail
 * @param excludeCredentialIds as digitais que a pessoa já tem — o aparelho
 *                    recusa cadastrar a mesma duas vezes
 */
public record RegistrationOptionsDTO(
        UUID challengeId,
        String challenge,
        String rpId,
        String rpName,
        String userId,
        String userName,
        String userDisplayName,
        List<String> excludeCredentialIds,
        long timeoutMs
) {}
