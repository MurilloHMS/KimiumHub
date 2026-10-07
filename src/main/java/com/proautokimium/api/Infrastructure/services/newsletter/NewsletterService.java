package com.proautokimium.api.Infrastructure.services.newsletter;

import com.proautokimium.api.Infrastructure.converters.NewsletterConverter;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeMessage;
import jakarta.transaction.Transactional;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

import com.proautokimium.api.Application.DTOs.email.NewsletterResponseDTO;
import com.proautokimium.api.Infrastructure.repositories.NewsletterRepository;
import com.proautokimium.api.Infrastructure.services.email.EmailRenderer;
import com.proautokimium.api.Infrastructure.repositories.SmtpEmailRepository;
import com.proautokimium.api.domain.entities.EmailEntity;
import com.proautokimium.api.domain.entities.Newsletter;
import com.proautokimium.api.domain.enums.EmailStatus;

import java.io.UnsupportedEncodingException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class NewsletterService {

    private static final String TEMPLATE_NAME= "html/newsletter_v2";
    private final JavaMailSender mailSender;
    private final EmailRenderer renderer;
    
    private final NewsletterRepository repository;
    private final SmtpEmailRepository emailRepository;
    private final NewsletterConverter converter;
    

    public NewsletterService(JavaMailSender mailSender,
                             EmailRenderer renderer,
                             NewsletterRepository repository,
                             SmtpEmailRepository emailRepository,
                             NewsletterConverter converter) {
        this.mailSender = mailSender;
        this.renderer = renderer;
        this.repository = repository;
        this.emailRepository = emailRepository;
        this.converter = converter;
    }

    @Transactional
    public void setReadyToSend(){
        List<Newsletter> allByStatus = repository.findAllByStatus(EmailStatus.PENDING);
        allByStatus.forEach(Newsletter::setScheduled);
    }
    
    public List<NewsletterResponseDTO> getAllPendingEmails(){
    	return repository.findAllByStatus(EmailStatus.PENDING)
    			.stream()
    			.map(converter::toDto).toList();
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

        // HashMap, e não Map.of: são 15 valores (o Map.of vai só até 10) e os
        // textos (mês, cliente, produto em destaque) podem vir null da planilha.
        Map<String, Object> vars = new HashMap<>();
        vars.put("mes", newsletter.getMes());
        vars.put("nomeDoCliente", newsletter.getNomeDoCliente());
        vars.put("produtoEmDestaque", newsletter.getProdutoEmDestaque());
        vars.put("quantidadeDeProdutos", newsletter.getQuantidadeDeProdutos());
        vars.put("quantidadeDeLitros", newsletter.getQuantidadeDeLitros());
        vars.put("quantidadeDeVisitas", newsletter.getQuantidadeDeVisitas());
        vars.put("valorDePecasTrocadas", newsletter.getValorDePecasTrocadas());

        // Total de horas: normais + mau uso.
        vars.put("valorTotalDeHoras", newsletter.getValorTotalDeHoras() + newsletter.getValorTotalDeHorasMauUso());
        vars.put("valorTotalCobradoHoras", newsletter.getValorTotalCobradoHoras() + newsletter.getValorTotalCobradoHorasMauUso());
        vars.put("horasNormais", newsletter.getValorTotalDeHoras());
        vars.put("valorHorasNormais", newsletter.getValorTotalCobradoHoras());
        vars.put("horasMauUso", newsletter.getValorTotalDeHorasMauUso());
        vars.put("valorHorasMauUso", newsletter.getValorTotalCobradoHorasMauUso());

        vars.put("mediaDiasAtendimento", newsletter.getMediaDiasAtendimento());
        vars.put("faturamentoTotal", newsletter.getFaturamentoTotal());

        // O logo é o do layout comum (texto). Sem addInline: imagem embutida que o
        // HTML não usa aparece como anexo em alguns programas de e-mail.
        email.setText(renderer.render(TEMPLATE_NAME, vars), true);

        mailSender.send(mimeMessage);
    }
    
    private String capitalizeMonth(String str) {
    	if(str == null || str.isEmpty())
    		return str;
    	
    	return str.substring(0,1).toUpperCase() + str.substring(1);
    }
}
