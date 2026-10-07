package com.proautokimium.api.Infrastructure.factories;

import com.proautokimium.api.Infrastructure.services.email.EmailRenderer;
import com.proautokimium.api.domain.entities.email.EmailQueue;
import com.proautokimium.api.domain.enums.email.EmailHrSubjects;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;
import org.thymeleaf.spring6.SpringTemplateEngine;
import org.thymeleaf.templateresolver.ClassLoaderTemplateResolver;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * A fábrica dos e-mails de candidatura, com o motor de verdade: o que sai é o
 * HTML do template no casco, não mais o text block do Java.
 */
class EmailFactoryTest {

    private EmailFactory factory;

    @BeforeEach
    void setUp() {
        ClassLoaderTemplateResolver resolver = new ClassLoaderTemplateResolver();
        resolver.setPrefix("templates/");
        resolver.setSuffix(".html");
        resolver.setTemplateMode("HTML");
        resolver.setCharacterEncoding("UTF-8");
        SpringTemplateEngine engine = new SpringTemplateEngine();
        engine.setTemplateResolver(resolver);

        factory = new EmailFactory(new EmailRenderer(engine));
        // O remetente vem do @Value; no teste, preenchido à mão.
        ReflectionTestUtils.setField(factory, "from", "noreply@envios.proautokimium.com.br");
    }

    @Test
    @DisplayName("candidatura recebida: destinatário, remetente, assunto e o template no casco")
    void candidaturaConfirmada() {
        String aviso = "<p>Usamos o currículo que você já tinha enviado. <a href=\"https://site/meu-curriculo\">acessar meu cadastro</a></p>";

        EmailQueue email = factory.candidaturaConfirmada("camila@email.com", "Camila", "Técnico de Manutenção", aviso);

        assertThat(email.getToEmail()).isEqualTo("camila@email.com");
        assertThat(email.getFromEmail()).isEqualTo("noreply@envios.proautokimium.com.br");
        assertThat(email.getSubject()).isEqualTo(EmailHrSubjects.APPLICATION_RECEIVED.getText());
        assertThat(email.getBody())
                .contains("Proauto <span style=\"color:#57c1ab;\">Kimium</span>")   // o casco
                .contains(">Olá, Camila</p>")
                .contains("vaga de Técnico de Manutenção")
                .contains("<a href=\"https://site/meu-curriculo\">acessar meu cadastro</a>");
    }

    @Test
    @DisplayName("candidatura recebida sem aviso: o bloco do aviso não aparece")
    void candidaturaConfirmadaSemAviso() {
        EmailQueue email = factory.candidaturaConfirmada("camila@email.com", "Camila", "Técnico de Manutenção", "");

        assertThat(email.getBody()).doesNotContain("Aviso sobre o currículo");
    }

    @Test
    @DisplayName("aprovada, reprovada e avançou: cada uma com o seu template e o seu assunto")
    void asOutrasTres() {
        EmailQueue aprovada = factory.candidaturaAprovada("camila@email.com", "Camila", "Técnico de Manutenção");
        EmailQueue reprovada = factory.candidaturaReprovada("camila@email.com", "Camila", "Técnico de Manutenção");
        EmailQueue avancou = factory.avancoEtapa("camila@email.com", "Camila", "Técnico de Manutenção");

        assertThat(aprovada.getSubject()).isEqualTo(EmailHrSubjects.WELCOME.getText());
        assertThat(aprovada.getBody()).contains(">Você foi aprovado(a)!</h1>").contains(">Parabéns, Camila</p>");

        assertThat(reprovada.getSubject()).isEqualTo(EmailHrSubjects.REJECTED.getText());
        assertThat(reprovada.getBody()).contains(">Obrigado por participar</h1>").contains("Técnico de Manutenção");

        assertThat(avancou.getSubject()).isEqualTo(EmailHrSubjects.NEXT_STAGE.getText());
        assertThat(avancou.getBody()).contains(">Você avançou para a próxima etapa</h1>").contains(">Parabéns, Camila</p>");

        for (EmailQueue e : java.util.List.of(aprovada, reprovada, avancou)) {
            assertThat(e.getToEmail()).isEqualTo("camila@email.com");
            assertThat(e.getBody()).contains("Proauto <span style=\"color:#57c1ab;\">Kimium</span>");
        }
    }
}
