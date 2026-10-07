package com.proautokimium.api.domain.enums.email;

import lombok.Getter;

/**
 * De onde vem cada e-mail do ERP. É o que a fila guarda em {@code origin}, o
 * que a tela filtra e o que escolhe o remetente (tabela {@code email_routes}).
 *
 * {@code sensitive}: o corpo leva código ou link que dá acesso à conta de
 * outra pessoa — a tela do desenvolvedor mostra que ele existe, sem o conteúdo.
 */
@Getter
public enum EmailOrigin {
    FIRST_ACCESS("Primeiro acesso", "código de 6 dígitos, sai na hora", true),
    PASSWORD_RESET("Redefinição de senha", "sai na hora", true),
    CLIENT_INVITE("Convite da Área do Cliente", "link para definir a senha", true),
    TALENT_BANK("Banco de talentos", "link para os dados do candidato", true),
    RECRUITMENT("Candidaturas", "recebida, avançou, aprovada, não seguiu", false),
    CHECKLIST("Checklist de vendas", "para a Controladoria", false),
    MACHINE_ALERT("Alertas de máquinas", "resumo diário da programação", false),
    DOCUMENT_ALERT("Vencimento de documentos", "aviso ao responsável", false),
    REIMBURSEMENT_REPORT("Relatório de reembolsos", "com PDF anexo", false),
    NEWSLETTER("Newsletter", "resumo mensal do cliente", false),
    MANUAL("Envio manual", "tela de Comunicação", false);

    private final String label;
    private final String hint;
    private final boolean sensitive;

    EmailOrigin(String label, String hint, boolean sensitive) {
        this.label = label;
        this.hint = hint;
        this.sensitive = sensitive;
    }
}
