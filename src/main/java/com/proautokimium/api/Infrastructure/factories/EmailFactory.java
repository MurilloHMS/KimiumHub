package com.proautokimium.api.Infrastructure.factories;

import com.proautokimium.api.Infrastructure.services.email.EmailRenderer;
import com.proautokimium.api.domain.entities.email.EmailQueue;
import com.proautokimium.api.domain.enums.email.EmailHrSubjects;
import com.proautokimium.api.domain.enums.email.EmailOrigin;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.Map;

@Component
public class EmailFactory {

    private final EmailRenderer renderer;
    private final Clock clock;

    // O remetente não é mais daqui: a fila resolve pela origem (RECRUITMENT) na hora de enfileirar.
    public EmailFactory(EmailRenderer renderer, Clock clock){
        this.renderer = renderer;
        this.clock = clock;
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
        return EmailQueue.of(EmailOrigin.RECRUITMENT, to, EmailHrSubjects.APPLICATION_RECEIVED.getText(), html, LocalDateTime.now(clock));
    }
    
    public EmailQueue candidaturaAprovada(String to, String nome, String vaga){
        String html = renderer.render("html/candidatura-aprovada", Map.of("nome", nome, "vaga", vaga));
        return EmailQueue.of(EmailOrigin.RECRUITMENT, to, EmailHrSubjects.WELCOME.getText(), html, LocalDateTime.now(clock));
    }

    public EmailQueue candidaturaReprovada(String to, String nome, String vaga){
        String html = renderer.render("html/candidatura-reprovada", Map.of("nome", nome, "vaga", vaga));
        return EmailQueue.of(EmailOrigin.RECRUITMENT, to, EmailHrSubjects.REJECTED.getText(), html, LocalDateTime.now(clock));
    }

    public EmailQueue avancoEtapa(String to, String nome, String vaga){
        String html = renderer.render("html/candidatura-avancou", Map.of("nome", nome, "vaga", vaga));
        return EmailQueue.of(EmailOrigin.RECRUITMENT, to, EmailHrSubjects.NEXT_STAGE.getText(), html, LocalDateTime.now(clock));
    }
}
