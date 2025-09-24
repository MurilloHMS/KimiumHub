package com.proautokimium.api.domain.enums;

import lombok.Getter;

@Getter
public enum Hierarchy {
    DIRETOR("diretor"),
    CEO("ceo"),
    SUPERVISOR("supervisor"),
    GERENTE("gerente"),
    COORDENADOR("coordenador"),
    ANALISTA("analista"),
    ASSISTENTE("assistente");

    private final String hierarquia;

    Hierarchy(String hierarquia){ this.hierarquia = hierarquia;}

}
