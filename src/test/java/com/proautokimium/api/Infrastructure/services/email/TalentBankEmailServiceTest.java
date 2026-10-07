package com.proautokimium.api.Infrastructure.services.email;

import com.proautokimium.api.domain.enums.email.EmailOrigin;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.spring6.SpringTemplateEngine;
import org.thymeleaf.templateresolver.ClassLoaderTemplateResolver;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

/**
 * Renderiza o template <b>de verdade</b>, e não um mock do {@code TemplateEngine}.
 *
 * <p>O aviso de vencimento sai de um agendador às 9h. Um erro de expressão no
 * Thymeleaf não aparece na subida nem em teste com mock — aparece no log da
 * manhã, com ninguém avisado.
 */
class TalentBankEmailServiceTest {

    private final EmailQueueService fila = mock(EmailQueueService.class);
    private final TalentBankEmailService service =
            new TalentBankEmailService(new EmailRenderer(motorReal()), fila, "https://site.teste");

    private static TemplateEngine motorReal() {
        ClassLoaderTemplateResolver resolver = new ClassLoaderTemplateResolver();
        resolver.setPrefix("templates/");
        resolver.setSuffix(".html");
        resolver.setCharacterEncoding("UTF-8");
        // O Spring, e não o TemplateEngine puro: é SpringEL que avalia as
        // expressões na aplicação. O puro usaria OGNL, que nem está no classpath.
        TemplateEngine engine = new SpringTemplateEngine();
        engine.setTemplateResolver(resolver);
        return engine;
    }

    private String corpoEnviado(String assuntoEsperado) {
        ArgumentCaptor<String> corpo = ArgumentCaptor.forClass(String.class);
        verify(fila).enqueue(eq(EmailOrigin.TALENT_BANK), eq("maria@email.com"), eq(assuntoEsperado), corpo.capture());
        return corpo.getValue();
    }

    @Test
    @DisplayName("Aviso de vencimento renderiza a data e o link com token, e vai para a fila")
    void avisoComToken() {
        service.enviarAvisoDeExpiracao("maria@email.com", "Maria Souza",
                LocalDateTime.of(2028, 9, 11, 9, 0), Optional.of("tok123"));

        String html = corpoEnviado("Seus dados no Banco de Talentos vencem em 11/09/2028");

        assertThat(html).contains("11/09/2028");
        assertThat(html).as("so o primeiro nome").contains(">Olá, Maria<").doesNotContain("Souza");
        assertThat(html).contains("href=\"https://site.teste/meu-curriculo/tok123\"");
        assertThat(html).contains("O link expira em 24 horas");
        verify(fila, never()).sendNow(eq(EmailOrigin.TALENT_BANK), org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any());
    }

    /**
     * Sem token (o cooldown recusou emitir), o aviso sai do mesmo jeito. O botão
     * não pode ficar sem destino, e a frase de "expira em 24 horas" mentiria.
     */
    @Test
    @DisplayName("Sem token, o aviso leva para pedir o link e nao fala em prazo de link")
    void avisoSemToken() {
        service.enviarAvisoDeExpiracao("maria@email.com", "Maria Souza",
                LocalDateTime.of(2028, 9, 11, 9, 0), Optional.empty());

        String html = corpoEnviado("Seus dados no Banco de Talentos vencem em 11/09/2028");

        assertThat(html).contains("href=\"https://site.teste/meu-curriculo\"");
        assertThat(html).doesNotContain("expira em 24 horas");
    }

    @Test
    @DisplayName("cadastro sem nome: o e-mail sai com Olá, sem quebrar (o Map.of recusaria o null)")
    void semNome() {
        service.enviarLinkDeAcesso("maria@email.com", "  ", "tok123");
        service.enviarAvisoDeExpiracao("maria@email.com", null, LocalDateTime.of(2028, 9, 11, 9, 0), Optional.empty());

        ArgumentCaptor<String> corpo = ArgumentCaptor.forClass(String.class);
        verify(fila, org.mockito.Mockito.times(2)).enqueue(eq(EmailOrigin.TALENT_BANK), eq("maria@email.com"), org.mockito.ArgumentMatchers.anyString(), corpo.capture());
        assertThat(corpo.getAllValues()).allSatisfy(html ->
                assertThat(html).contains(">Olá</p>").doesNotContain("null"));
        assertThat(corpo.getAllValues().get(0)).contains("href=\"https://site.teste/meu-curriculo/tok123\"");
    }
}
