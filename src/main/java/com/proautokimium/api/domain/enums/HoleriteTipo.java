package com.proautokimium.api.domain.enums;

public enum HoleriteTipo {
    ADIANTAMENTO("adiantamento"),
    SALARIO("salário"),
    DECIMO_TERCEIRO_1("13º — 1ª parcela"),
    DECIMO_TERCEIRO_2("13º — 2ª parcela");

    private final String label;

    HoleriteTipo(String label) {
        this.label = label;
    }

    /**
     * Como o tipo aparece para o funcionário, no aviso e na tela dele.
     *
     * Fica aqui e não em quem usa: o rótulo estava cravado num ternário no
     * serviço de notificação, e com dois tipos novos ele passaria a chamar 13º
     * de salário sem ninguém perceber.
     */
    public String getLabel() {
        return label;
    }
}