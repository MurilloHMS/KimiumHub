package com.proautokimium.api.domain.enums.humanResources;

public enum HoleritePreviewStatus {
    /** Funcionário cadastrado, sem holerite deste tipo na competência: vai ser enviado. */
    PRONTO,
    /** CPF não bate com nenhum funcionário. Cadastrar e reenviar o mesmo PDF. */
    NAO_CADASTRADO,
    /** Já existe holerite deste tipo e competência. Vai ser pulado. */
    JA_ENVIADO,
    /** O CPF aparece em mais de um cadastro — a página fica de fora até resolverem. */
    CPF_DUPLICADO,
    /** Não deu para ler o CPF da página. Provável PDF digitalizado ou layout novo. */
    CPF_ILEGIVEL,
    /** Cadastrado, mas sem login: recebe o holerite e não é avisado por ninguém. */
    SEM_USUARIO
}