package com.proautokimium.api.domain.enums;

/** Categoria da notificação — define ícone/cor no frontend e permite filtragem. */
public enum NotificationType {
    /** Novos holerites disponibilizados pelo RH. */
    HOLERITE,
    /** Novo documento vinculado pelo RH ao funcionário. */
    DOCUMENTO,
    /** Mudança de status (aprovado/reprovado/pago) numa solicitação de reembolso. */
    REEMBOLSO,
    /** Atestado: chegou um para o RH conferir, ou o RH confirmou ou recusou. */
    ATESTADO,
    /** Mensagem composta manualmente pelo RH pra um ou mais funcionários. */
    PERSONALIZADA,
    /** Checklist de vendas: chegou um para a Controladoria, ou a Controladoria respondeu. */
    CHECKLIST,
    /** Evento da empresa: o lembrete diário para quem ainda não respondeu ao convite. */
    EVENTO,
    /** O resumo diário da programação de máquinas: atrasadas e saídas próximas. */
    PROGRAMACAO,
    /** Solicitação do RH: chegou uma para responder, ou o RH aprovou ou devolveu a resposta. */
    SOLICITACAO,
    /** Notificação genérica do sistema. */
    GERAL
}
