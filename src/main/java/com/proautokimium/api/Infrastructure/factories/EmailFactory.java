package com.proautokimium.api.Infrastructure.factories;

import com.proautokimium.api.domain.entities.email.EmailQueue;
import com.proautokimium.api.domain.models.EmailTemplates;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class EmailFactory {
    @Value("${mail.from}")
    private String from;

    public EmailQueue candidaturaConfirmada(String to, String nome, String vaga){
        return new EmailQueue(
                to,
                from,
                EmailTemplates.Subjects.CONFIRMACAO_CANDIDATURA,
                EmailTemplates.confirmacaoCandidatura(nome, vaga)
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
