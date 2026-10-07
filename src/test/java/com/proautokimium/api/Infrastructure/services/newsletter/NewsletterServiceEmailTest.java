package com.proautokimium.api.Infrastructure.services.newsletter;

import com.proautokimium.api.Infrastructure.converters.NewsletterConverter;
import com.proautokimium.api.Infrastructure.repositories.NewsletterRepository;
import com.proautokimium.api.Infrastructure.services.email.EmailQueueService;
import com.proautokimium.api.Infrastructure.services.email.EmailRenderer;
import com.proautokimium.api.domain.entities.Newsletter;
import com.proautokimium.api.domain.enums.email.EmailOrigin;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.thymeleaf.spring6.SpringTemplateEngine;
import org.thymeleaf.templateresolver.ClassLoaderTemplateResolver;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

/**
 * A newsletter vai para a fila de e-mail (origem NEWSLETTER) com o HTML do
 * layout comum: sem fila própria, sem falar direto com o SMTP, sem imagem embutida.
 */
class NewsletterServiceEmailTest {

    @Test
    @DisplayName("newsletter: entra na fila com o HTML do casco e o dinheiro no formato do Brasil")
    void entraNaFila() {
        ClassLoaderTemplateResolver resolver = new ClassLoaderTemplateResolver();
        resolver.setPrefix("templates/");
        resolver.setSuffix(".html");
        resolver.setCharacterEncoding("UTF-8");
        SpringTemplateEngine engine = new SpringTemplateEngine();
        engine.setTemplateResolver(resolver);
        EmailQueueService fila = mock(EmailQueueService.class);

        NewsletterService service = new NewsletterService(fila, new EmailRenderer(engine),
                mock(NewsletterRepository.class), mock(NewsletterConverter.class));

        Newsletter n = new Newsletter();
        n.setMes("setembro");
        n.setNomeDoCliente("Lavanderia Central");
        n.setEmailCliente("compras@lavanderia.com.br");
        n.setFaturamentoTotal(48250.75);
        // produtoEmDestaque fica null, como vem da planilha às vezes: o Map.of quebraria.

        service.enqueue(n);

        ArgumentCaptor<String> html = ArgumentCaptor.forClass(String.class);
        verify(fila).enqueue(eq(EmailOrigin.NEWSLETTER), eq("compras@lavanderia.com.br"),
                eq("Setembro trouxe surpresas - Veja seus resultados!"), html.capture());
        assertThat(html.getValue()).contains("Proauto <span style=\"color:#57c1ab;\">Kimium</span>")
                .contains(">Lavanderia Central</p>").contains(">R$ 48.250,75</td>").doesNotContain("cid:");
    }
}
