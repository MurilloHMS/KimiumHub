package com.proautokimium.api.Infrastructure.services.email.newsletter;

import java.io.IOException;
import java.util.List;

import com.proautokimium.api.Infrastructure.abstractions.excel.SheetSource;
import com.proautokimium.api.Infrastructure.abstractions.excel.SheetSourceFactory;
import com.proautokimium.api.Infrastructure.exceptions.newsletter.NewsletterFileNotValidException;
import com.proautokimium.api.Infrastructure.exceptions.newsletter.NewsletterNullException;
import com.proautokimium.api.Infrastructure.services.email.newsletter.reader.NewsletterOneFileReaderService;
import com.proautokimium.api.domain.entities.Newsletter;
import com.proautokimium.api.domain.enums.EmailStatus;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import com.proautokimium.api.Infrastructure.interfaces.email.newsletter.INewsletterOrchestrator;
import com.proautokimium.api.Infrastructure.repositories.NewsletterRepository;

@Service
public class NewsletterOrchestratorService implements INewsletterOrchestrator {


	private final NewsletterRepository repository;
    private final NewsletterOneFileReaderService newsletterOneFileReaderService;
    private final NewsletterService service;
    private static final Logger LOGGER = LoggerFactory.getLogger(NewsletterOrchestratorService.class);
    private final SheetSourceFactory sheetSourceFactory;

    public NewsletterOrchestratorService(NewsletterOneFileReaderService newsletterOneFileReaderService,
                                         NewsletterRepository repository,
                                         NewsletterService service, SheetSourceFactory sheetSourceFactory) {
        this.newsletterOneFileReaderService = newsletterOneFileReaderService;
        this.repository = repository;
        this.service = service;
        this.sheetSourceFactory = sheetSourceFactory;
    }

    @Override
    public void includeMonthlyNewsletterByExcel(MultipartFile file) {
            if(file == null || file.isEmpty())
                throw new NewsletterFileNotValidException("Arquivo não encontrado");

        try {
            SheetSource source = sheetSourceFactory.forFile(file.getOriginalFilename());

            List<Newsletter> newsletters = source.read(file.getInputStream())
                    .stream()
                    .map(newsletterOneFileReaderService::mapRow)
                    .filter(n -> n.getCodigoCliente() != null && n.getCodigoCliente().matches("\\d+"))
                    .toList();

            if (newsletters.isEmpty()) {
                throw new NewsletterNullException("Nenhum dado válido encontrado na planilha.");
            }
            repository.saveAll(newsletters);
        } catch (IOException e) {
            throw new NewsletterFileNotValidException("Erro ao ler o arquivo enviado.");
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Override
	public void executeMonthlyNewsletter() {
		List<Newsletter> newslettersToSend = repository.findTop15ByStatusIn(List.of(EmailStatus.SCHEDULED, EmailStatus.RETRYING));
        if(newslettersToSend == null ||newslettersToSend.isEmpty())
            return;

        int success = 0;
        int error = 0;

		LOGGER.info("Iniciando envios de emails");
		for(Newsletter newsletter: newslettersToSend) {
			try {
				service.sendMailWithInline(newsletter);
				newsletter.setStatus(EmailStatus.SENT);
                success++;
			}catch (Exception e) {
				newsletter.setStatus(EmailStatus.ERROR);
				LOGGER.error("Erro ao enviar newsletter para " + newsletter.getCodigoCliente() + ": " + e.getMessage());
                error++;
			}finally {
				repository.save(newsletter);
			}
		}
		LOGGER.info("Envio de Informativos finalizados!\n\nSucesso: {}\nError: {}",success, error );
	}
}
