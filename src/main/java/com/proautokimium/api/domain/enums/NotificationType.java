package com.proautokimium.api.domain.enums;

/** Categoria da notificação — define ícone/cor no frontend e permite filtragem. */
public enum NotificationType {
    /** Novos holerites disponibilizados pelo RH. */
    HOLERITE,
    /** Novo documento vinculado pelo RH ao funcionário. */
    DOCUMENTO,
    /** Mudança de status (aprovado/reprovado/pago) numa solicitação de reembolso. */
    REEMBOLSO,
    /** Mensagem composta manualmente pelo RH pra um ou mais funcionários. */
    PERSONALIZADA,
    /** Checklist de vendas: chegou um para a Controladoria, ou a Controladoria respondeu. */
    CHECKLIST,
    /** Notificação genérica do sistema. */
    GERAL
}
