package com.proautokimium.api.domain.enums.home;

/**
 * O que pode estar esperando alguém na home.
 *
 * O tipo viaja, a rota não: para onde cada pendência leva é assunto do front,
 * que já conhece as próprias rotas. Mandar "/documentos/holerites" daqui
 * acoplaria a API ao roteador do Angular.
 */
public enum PendingType {
    HOLERITE_NAO_CONFIRMADO,
    FERIAS_AGUARDANDO,
    REEMBOLSO_AGUARDANDO,
    APROVACAO_FERIAS,
    APROVACAO_REEMBOLSO
}
