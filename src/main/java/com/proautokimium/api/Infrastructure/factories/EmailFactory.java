package com.proautokimium.api.Infrastructure.factories;

import com.proautokimium.api.Infrastructure.services.email.EmailRenderer;
import com.proautokimium.api.domain.entities.email.EmailQueue;
import com.proautokimium.api.domain.enums.email.EmailHrSubjects;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

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
     *
     * <p>O aviso é o que responde ao caso "já estou no banco e me candidatei
     * de novo": <b>a pergunta viaja por e-mail</b>, e não pela tela. Perguntar
     * na tela exigiria dizer à pessoa o que temos guardado, e essa frase é um
     * oráculo de enumeração numa rota pública.
     *
     * @param aviso HTML já pronto (montado pelo código, nunca pelo candidato),
     *              ou vazio quando não há nada a dizer
     */
    public EmailQueue candidaturaConfirmada(String to, String nome, String vaga, String aviso){
        String html = renderer.render("html/candidatura-recebida", Map.of("nome", nome, "vaga", vaga, "aviso",aviso));
        return new EmailQueue(to, from, EmailHrSubjects.APPLICATION_RECEIVED.getText(), html);
    }
    
    public EmailQueue candidaturaAprovada(String to, String nome, String vaga){
        String html = renderer.render("html/candidatura-aprovada", Map.of("nome", nome, "vaga", vaga));
        return new EmailQueue(to, from, EmailHrSubjects.WELCOME.getText(), html);
    }

    public EmailQueue candidaturaReprovada(String to, String nome, String vaga){
        String html = renderer.render("html/candidatura-reprovada", Map.of("nome", nome, "vaga", vaga));
        return new EmailQueue(to, from, EmailHrSubjects.REJECTED.getText(), html);
    }

    public EmailQueue avancoEtapa(String to, String nome, String vaga){
        String html = renderer.render("html/candidatura-avancou", Map.of("nome", nome, "vaga", vaga));
        return new EmailQueue(to, from, EmailHrSubjects.NEXT_STAGE.getText(), html);
    }
}
