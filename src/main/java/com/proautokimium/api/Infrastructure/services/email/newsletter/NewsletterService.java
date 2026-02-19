package com.proautokimium.api.Infrastructure.services.email.newsletter;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeMessage;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.core.io.ClassPathResource;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

import com.proautokimium.api.Application.DTOs.email.NewsletterResponseDTO;
import com.proautokimium.api.Infrastructure.repositories.NewsletterRepository;
import com.proautokimium.api.Infrastructure.repositories.SmtpEmailRepository;
import com.proautokimium.api.domain.entities.EmailEntity;
import com.proautokimium.api.domain.entities.Newsletter;
import com.proautokimium.api.domain.enums.EmailStatus;

import java.io.UnsupportedEncodingException;
import java.util.List;

@Service
public class NewsletterService {

    private static final String TEMPLATE_NAME= "html/newsletter_v2";
    private static final String PROAUTO_LOGO_IMAGE= "templates/images/logo.png";
    private static final String PNG_MIME= "image/png";
    private final JavaMailSender mailSender;
    private final TemplateEngine htmlTemplateEngine;
    
    private final NewsletterRepository repository;
    private final SmtpEmailRepository emailRepository;
    

    public NewsletterService(JavaMailSender mailSender, TemplateEngine htmlTemplateEngine, NewsletterRepository repository, SmtpEmailRepository emailRepository) {
        this.mailSender = mailSender;
        this.htmlTemplateEngine = htmlTemplateEngine;
        this.repository = repository;
        this.emailRepository = emailRepository;
    }
    
    public List<NewsletterResponseDTO> getAllPendingEmails(){
    	return repository.findAllByStatus(EmailStatus.PENDING)
    			.stream()
    			.map(m -> new NewsletterResponseDTO(
    					m.getCodigoCliente(),
    					m.getNomeDoCliente(),
    					m.getData(),
    					m.getMes(),
    					m.getQuantidadeDeProdutos(),
    					m.getQuantidadeDeLitros(),
    					m.getQuantidadeNotasEmitidas(),
    					m.getMediaDiasAtendimento(),
    					m.getProdutoEmDestaque(),
    					m.getFaturamentoTotal(),
    					m.getValorDePecasTrocadas(),
    					m.getValorTotalDeHoras(),
    					m.getValorTotalCobradoHoras(),
    					m.isMauUso(),
    					m.getValorTotalDeHorasMauUso(),
    					m.getValorTotalCobradoHorasMauUso(),
    					m.getStatus(),
    					m.getEmailCliente(),
                        m.getMatrizCode(),
                        m.getMatrizName()
    					)).toList();
    }

    public void sendMailWithInline(Newsletter newsletter) throws MessagingException, UnsupportedEncodingException{
    	EmailEntity newsletterEmail = emailRepository.findByName("newsletter");
    	
        String mailFrom = newsletterEmail.getEmail().getAddress();
        String mailFromName = "Proauto Kimium";

        final MimeMessage mimeMessage = this.mailSender.createMimeMessage();
        final MimeMessageHelper email;
        email = new MimeMessageHelper(mimeMessage, true, "UTF-8");
        
        String capMonth = capitalizeMonth(newsletter.getMes());

        email.setTo(newsletter.getEmailCliente());
        email.setSubject(capMonth + " trouxe surpresas - Veja seus resultados!");
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
        
        // Total Horas
        double totalHoras = newsletter.getValorTotalDeHoras() + newsletter.getValorTotalDeHorasMauUso();
        ctx.setVariable("valorTotalDeHoras", totalHoras);
        double totalCobrado = newsletter.getValorTotalCobradoHoras() + newsletter.getValorTotalCobradoHorasMauUso();
        ctx.setVariable("valorTotalCobradoHoras", totalCobrado);
        
        // Total Horas Normais
        ctx.setVariable("horasNormais", newsletter.getValorTotalDeHoras());
        ctx.setVariable("valorHorasNormais", newsletter.getValorTotalCobradoHoras());
        
        // Total Horas Mau Uso
        ctx.setVariable("horasMauUso", newsletter.getValorTotalDeHorasMauUso());
        ctx.setVariable("valorHorasMauUso", newsletter.getValorTotalCobradoHorasMauUso());
        
        ctx.setVariable("mediaDiasAtendimento", newsletter.getMediaDiasAtendimento());
        ctx.setVariable("faturamentoTotal", newsletter.getFaturamentoTotal());

        final String htmlContent = this.htmlTemplateEngine.process(TEMPLATE_NAME, ctx);

        email.setText(htmlContent, true);

        ClassPathResource clr = new ClassPathResource(PROAUTO_LOGO_IMAGE);

        email.addInline("proautoLogo", clr, PNG_MIME);

        mailSender.send(mimeMessage);
    }
    
    private String capitalizeMonth(String str) {
    	if(str == null || str.isEmpty())
    		return str;
    	
    	return str.substring(0,1).toUpperCase() + str.substring(1);
    }
}
