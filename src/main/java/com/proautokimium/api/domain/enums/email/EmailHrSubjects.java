package com.proautokimium.api.domain.enums.email;

import lombok.Getter;

@Getter
public enum EmailHrSubjects {
    APPLICATION_RECEIVED("Recebemos sua candidatura"),
    NEXT_STAGE("Você avançou para a próxima etapa"),
    REJECTED("Atualização sobre seu processo seletivo"),
    APPROVED("Parabéns! Você foi aprovado(a)"),
    PROPOSAL("Proposta - Proauto Kimium"),
    WELCOME("Bem-vindo(a) à Proauto Kimium");

    private final String text;
    EmailHrSubjects(String text) { this.text = text;}
}
