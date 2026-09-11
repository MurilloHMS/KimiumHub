package com.proautokimium.api.Infrastructure.factories;

import com.proautokimium.api.domain.entities.email.EmailQueue;
import com.proautokimium.api.domain.models.EmailTemplates;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class EmailFactory {
    @Value("${mail.from}")
    private String from;

    /**
     * A confirmacao com um aviso sobre o curriculo.
     *
     * <p>E por aqui que a pessoa que ja esta no banco fica sabendo o que
     * aconteceu com o curriculo dela -- se reaproveitamos o antigo, se
     * substituimos pelo novo, ou se nao achamos nenhum.
     */
    public EmailQueue candidaturaConfirmada(String to, String nome, String vaga, String aviso){
        return new EmailQueue(
                to,
                from,
                EmailTemplates.Subjects.CONFIRMACAO_CANDIDATURA,
                EmailTemplates.confirmacaoCandidatura(nome, vaga, aviso)
            );
    }
    public EmailQueue candidaturaAprovada(String to, String nome, String vaga){
        return new EmailQueue(
                to,
                from,
                EmailTemplates.Subjects.BOAS_VINDAS,
                EmailTemplates.aprovacao(nome, vaga)
        );
    }

    public EmailQueue candidaturaReprovada(String to, String nome, String vaga){
        return new EmailQueue(
                to,
                from,
                EmailTemplates.Subjects.REPROVACAO,
                EmailTemplates.reprovacao(nome, vaga)
        );
    }

    public EmailQueue avancoEtapa(String to, String nome, String vaga){
        return new EmailQueue(
                to,
                from,
                EmailTemplates.Subjects.AVANCO_ETAPA,
                EmailTemplates.avancouEtapa(nome, vaga)
        );
    }
}
