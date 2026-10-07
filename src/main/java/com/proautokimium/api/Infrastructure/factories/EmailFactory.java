package com.proautokimium.api.Infrastructure.factories;

import com.proautokimium.api.Infrastructure.services.email.EmailRenderer;
import com.proautokimium.api.domain.entities.email.EmailQueue;
import com.proautokimium.api.domain.models.EmailTemplates;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

import java.util.Map;

@Component
public class EmailFactory {
    @Value("${mail.from}")
    private String from;

    private final EmailRenderer renderer;

    public EmailFactory(EmailRenderer renderer){
        this.renderer = renderer;
    }

    /**
     * A confirmação, com um aviso sobre o currículo.
     *
     * <p>É por aqui que a pessoa que já está no banco fica sabendo o que
     * aconteceu com o currículo dela: se reaproveitamos o antigo, se
     * substituímos pelo novo, ou se não achamos nenhum.
     */
    public EmailQueue candidaturaConfirmada(String to, String nome, String vaga, String aviso){
        String html = renderer.render("html/candidatura-recebida", Map.of("nome", nome, "vaga", vaga, "aviso",aviso));
        return new EmailQueue(to, from, EmailTemplates.Subjects.CONFIRMACAO_CANDIDATURA, html);
    }
    
    public EmailQueue candidaturaAprovada(String to, String nome, String vaga){
        String html = renderer.render("html/candidatura-aprovada", Map.of("nome", nome, "vaga", vaga));
        return new EmailQueue(to, from, EmailTemplates.Subjects.APROVACAO, html);
    }

    public EmailQueue candidaturaReprovada(String to, String nome, String vaga){
        String html = renderer.render("html/candidatura-reprovada", Map.of("nome", nome, "vaga", vaga));
        return new EmailQueue(to, from, EmailTemplates.Subjects.REPROVACAO, html);
    }

    public EmailQueue avancoEtapa(String to, String nome, String vaga){
        String html = renderer.render("html/candidatura-avancou", Map.of("nome", nome, "vaga", vaga));
        return new EmailQueue(to, from, EmailTemplates.Subjects.AVANCO_ETAPA, html);
    }
}
