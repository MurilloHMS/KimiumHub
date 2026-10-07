package com.proautokimium.api.Infrastructure.services.newsletter;

import com.proautokimium.api.Infrastructure.converters.NewsletterConverter;
import com.proautokimium.api.Infrastructure.repositories.NewsletterRepository;
import com.proautokimium.api.Infrastructure.repositories.SmtpEmailRepository;
import com.proautokimium.api.Infrastructure.services.email.EmailRenderer;
import com.proautokimium.api.domain.entities.EmailEntity;
import com.proautokimium.api.domain.entities.Newsletter;
import com.proautokimium.api.domain.valueObjects.Email;
import jakarta.mail.BodyPart;
import jakarta.mail.Multipart;
import jakarta.mail.Part;
import jakarta.mail.Session;
import jakarta.mail.internet.MimeMessage;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.mail.javamail.JavaMailSender;
import org.thymeleaf.spring6.SpringTemplateEngine;
import org.thymeleaf.templateresolver.ClassLoaderTemplateResolver;

import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * A newsletter sai com o HTML do layout comum e SEM a imagem embutida: o logo
 * agora é texto, e imagem embutida que o HTML não usa aparece como anexo.
 */
class NewsletterServiceEmailTest {

    @Test
    @DisplayName("newsletter: HTML do casco, dinheiro no formato do Brasil e nenhuma imagem embutida")
    void semImagemEmbutida() throws Exception {
        ClassLoaderTemplateResolver resolver = new ClassLoaderTemplateResolver();
        resolver.setPrefix("templates/");
        resolver.setSuffix(".html");
        resolver.setCharacterEncoding("UTF-8");
        SpringTemplateEngine engine = new SpringTemplateEngine();
        engine.setTemplateResolver(resolver);

        JavaMailSender mailSender = mock(JavaMailSender.class);
        when(mailSender.createMimeMessage()).thenReturn(new MimeMessage(Session.getInstance(new Properties())));
        SmtpEmailRepository emails = mock(SmtpEmailRepository.class);
        EmailEntity remetente = new EmailEntity();
        remetente.setName("newsletter");
        remetente.setEmail(new Email("newsletter@envios.proautokimium.com.br"));
        when(emails.findByName("newsletter")).thenReturn(remetente);

        NewsletterService service = new NewsletterService(mailSender, new EmailRenderer(engine),
                mock(NewsletterRepository.class), emails, mock(NewsletterConverter.class));

        Newsletter n = new Newsletter();
        n.setMes("setembro");
        n.setNomeDoCliente("Lavanderia Central");
        n.setEmailCliente("compras@lavanderia.com.br");
        n.setFaturamentoTotal(48250.75);
        // produtoEmDestaque fica null, como vem da planilha às vezes: o Map.of quebraria.

        service.sendMailWithInline(n);

        ArgumentCaptor<MimeMessage> enviada = ArgumentCaptor.forClass(MimeMessage.class);
        verify(mailSender).send(enviada.capture());
        MimeMessage msg = enviada.getValue();
        msg.saveChanges();

        List<Part> partes = new ArrayList<>();
        coletar(msg, partes);
        String html = partes.stream().filter(p -> isHtml(p)).map(NewsletterServiceEmailTest::texto).findFirst().orElseThrow();

        assertThat(html).contains("Proauto <span style=\"color:#57c1ab;\">Kimium</span>")
                .contains(">Lavanderia Central</p>").contains(">R$ 48.250,75</td>");
        assertThat(partes).as("nenhuma imagem embutida").noneMatch(p -> tipo(p).startsWith("image/"));
    }

    private static void coletar(Part p, List<Part> out) throws Exception {
        out.add(p);
        if (p.getContent() instanceof Multipart mp) {
            for (int i = 0; i < mp.getCount(); i++) {
                BodyPart b = mp.getBodyPart(i);
                coletar(b, out);
            }
        }
    }

    private static boolean isHtml(Part p) {
        return tipo(p).startsWith("text/html");
    }

    private static String tipo(Part p) {
        try { return p.getContentType().toLowerCase(); } catch (Exception e) { return ""; }
    }

    private static String texto(Part p) {
        try { return (String) p.getContent(); } catch (Exception e) { throw new IllegalStateException(e); }
    }
}
