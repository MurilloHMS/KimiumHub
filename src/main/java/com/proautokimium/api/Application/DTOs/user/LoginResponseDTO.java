package com.proautokimium.api.Application.DTOs.user;

/**
 * O que o login devolve.
 *
 * Dois tokens com papéis diferentes: o `token` é o JWT de duas horas que
 * acompanha cada requisição, e o `refreshToken` é o de sete dias que só existe
 * para trocar por um `token` novo quando aquele vence.
 *
 * O refresh vai no corpo, e não num cookie `httpOnly`, porque o site e a API
 * estão em domínios diferentes — `proautokimium.com.br` e
 * `api.proautokimium.com`. O cookie seria de terceiros, e o Safari bloqueia
 * esses por padrão: funcionaria no Chrome do computador e falharia calado em
 * todo iPhone.
 */
public record LoginResponseDTO(String token, String refreshToken) {}
