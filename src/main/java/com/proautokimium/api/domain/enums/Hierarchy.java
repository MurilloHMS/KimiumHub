package com.proautokimium.api.domain.enums;

public enum Hierarchy {
    DIRETOR("diretor"),
    CEO("ceo"),
    SUPERVISOR("supervisor"),
    GERENTE("gerente"),
    COORDENADOR("coordenador"),
    ANALISTA("analista"),
    ASSISTENTE("assistente");

    private String hierarquia;

    Hierarchy(String hierarquia){ this.hierarquia = hierarquia;}

    public String getHierarquia() {
        return hierarquia;
    }
}
