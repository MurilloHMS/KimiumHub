package com.proautokimium.api.domain.enums;

public enum Hierarquia {
    DIRETOR("diretor"),
    CEO("ceo"),
    SUPERVISOR("supervisor"),
    GERENTE("gerente"),
    COORDENADOR("coordenador"),
    ANALISTA("analista"),
    ASSISTENTE("assistente");

    private String hierarquia;

    Hierarquia(String hierarquia){ this.hierarquia = hierarquia;}

    public String getHierarquia() {
        return hierarquia;
    }
}
