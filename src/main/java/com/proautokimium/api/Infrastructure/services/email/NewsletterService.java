package com.proautokimium.api.Infrastructure.services.email;

import com.proautokimium.api.domain.models.Newsletter;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeMessage;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.core.env.Environment;
import org.springframework.core.io.ClassPathResource;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

import java.io.UnsupportedEncodingException;

@Service
public class NewsletterService {

    private static final String TEMPLATE_NAME= "html/newsletter";
    private static final String PROAUTO_LOGO_IMAGE= "templates/images/logo.png";
    private static final String PNG_MIME= "image/png";

    private final Environment environment;
    private final JavaMailSender mailSender;
    private final TemplateEngine htmlTemplateEngine;

    public NewsletterService(Environment environment, JavaMailSender mailSender, TemplateEngine htmlTemplateEngine) {
        this.environment = environment;
        this.mailSender = mailSender;
        this.htmlTemplateEngine = htmlTemplateEngine;
    }

    public void sendMailWithInline(Newsletter newsletter) throws MessagingException, UnsupportedEncodingException{
        String confirmationUrl = "generated_confirmation_url";
        String mailFrom = environment.getProperty("spring.mail.properties.mail.smtp.from");
        String mailFromName = environment.getProperty("mail.from.name", "Proauto Kimium");

        final MimeMessage mimeMessage = this.mailSender.createMimeMessage();
        final MimeMessageHelper email;
        email = new MimeMessageHelper(mimeMessage, true, "UTF-8");

        email.setTo(newsletter.getEmailCliente());
        email.setSubject("Resumo Proauto Kimium - " + newsletter.getMes());
        email.setFrom(new InternetAddress(mailFrom,mailFromName));

        final Context ctx = new Context(LocaleContextHolder.getLocale());
        ctx.setVariable("mes", newsletter.getMes());
        ctx.setVariable("nomeDoCliente", newsletter.getNomeDoCliente());
        ctx.setVariable("proautoLogo", PROAUTO_LOGO_IMAGE);
        ctx.setVariable("produtoEmDestaque", newsletter.getProdutoEmDestaque());
        ctx.setVariable("quantidadeDeProdutos", newsletter.getQuantidadeDeProdutos());
        ctx.setVariable("quantidadeDeLitros", newsletter.getQuantidadeDeLitros());
        ctx.setVariable("quantidadeDeVisitas", newsletter.getQuantidadeDeVisitas());
        ctx.setVariable("valorDePecasTrocadas", newsletter.getValorDePecasTrocadas());
        ctx.setVariable("mediaDiasAtendimento", newsletter.getMediaDiasAtendimento());
        ctx.setVariable("faturamentoTotal", newsletter.getFaturamentoTotal());

        final String htmlContent = this.htmlTemplateEngine.process(TEMPLATE_NAME, ctx);

        email.setText(htmlContent, true);

        ClassPathResource clr = new ClassPathResource(PROAUTO_LOGO_IMAGE);

        email.addInline("proautoLogo", clr, PNG_MIME);

        mailSender.send(mimeMessage);
    }
}
