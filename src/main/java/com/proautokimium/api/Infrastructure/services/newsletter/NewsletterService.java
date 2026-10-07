package com.proautokimium.api.Infrastructure.services.newsletter;

import com.proautokimium.api.Infrastructure.converters.NewsletterConverter;
import jakarta.transaction.Transactional;
import org.springframework.stereotype.Service;

import com.proautokimium.api.Application.DTOs.email.NewsletterResponseDTO;
import com.proautokimium.api.Infrastructure.repositories.NewsletterRepository;
import com.proautokimium.api.Infrastructure.services.email.EmailQueueService;
import com.proautokimium.api.Infrastructure.services.email.EmailRenderer;
import com.proautokimium.api.domain.enums.email.EmailOrigin;
import com.proautokimium.api.domain.entities.Newsletter;
import com.proautokimium.api.domain.enums.EmailStatus;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class NewsletterService {

    private static final String TEMPLATE_NAME= "html/newsletter_v2";
    private final EmailQueueService emailQueue;
    private final EmailRenderer renderer;
    
    private final NewsletterRepository repository;
    private final NewsletterConverter converter;
    

    public NewsletterService(EmailQueueService emailQueue,
                             EmailRenderer renderer,
                             NewsletterRepository repository,
                             NewsletterConverter converter) {
        this.emailQueue = emailQueue;
        this.renderer = renderer;
        this.repository = repository;
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

    /**
     * Põe a newsletter do cliente na fila de e-mail (origem NEWSLETTER): o
     * remetente vem da configuração, e o envio, as tentativas e o motivo de
     * falha passam a ser os da fila, visíveis na tela do desenvolvedor.
     */
    public void enqueue(Newsletter newsletter) {
        String capMonth = capitalizeMonth(newsletter.getMes());

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
        emailQueue.enqueue(EmailOrigin.NEWSLETTER, newsletter.getEmailCliente(),
                capMonth + " trouxe surpresas - Veja seus resultados!", renderer.render(TEMPLATE_NAME, vars));
    }
    
    private String capitalizeMonth(String str) {
    	if(str == null || str.isEmpty())
    		return str;
    	
    	return str.substring(0,1).toUpperCase() + str.substring(1);
    }
}
