package com.proautokimium.api.domain.enums;

import lombok.Getter;

@Getter
public enum UserRole {
    ADMIN("admin"),
    USER("user"),
    RH("rh"),
    DEVELOPER("developer"),
    VENDEDOR("vendedor"),
    ADMINISTRATIVO("administrativo"),
    FINANCEIRO("financeiro"),
    DESIGN("design"),
    MARKETING("marketing"),
    CONTRATOS("contatos"),
    CLIENTE("cliente"),
    PARCEIRO("parceiro"),
    TECNICO("tecnico"),
    COMPRADOR("comprador"),
    MANUTENCAO("manutencao"),
    PRODUCAO("producao"),
    ALMOXARIFADO("almoxarifado");


    private final String role;

    UserRole(String role){
        this.role = role;
    }

}
