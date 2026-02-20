package com.proautokimium.api.Infrastructure.services.email.newsletter;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.List;

import com.proautokimium.api.Infrastructure.exceptions.newsletter.NewsletterFileNotValidException;
import com.proautokimium.api.Infrastructure.exceptions.newsletter.NewsletterNullException;
import com.proautokimium.api.domain.entities.Customer;
import com.proautokimium.api.domain.entities.Newsletter;
import com.proautokimium.api.domain.enums.EmailStatus;
import com.proautokimium.api.domain.models.newsletter.NewsletterExchangedParts;
import com.proautokimium.api.domain.models.newsletter.NewsletterNFeInfo;
import com.proautokimium.api.domain.models.newsletter.NewsletterServiceOrders;
import com.proautokimium.api.domain.models.newsletter.NewsletterTechnicalHours;

import jakarta.transaction.Transactional;

import org.apache.poi.EmptyFileException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import com.proautokimium.api.Application.DTOs.email.NewsletterData;
import com.proautokimium.api.Infrastructure.interfaces.email.newsletter.INewsletterOrchestrator;
import com.proautokimium.api.Infrastructure.repositories.CustomerRepository;
import com.proautokimium.api.Infrastructure.repositories.NewsletterRepository;

@Service
public class NewsletterOrchestratorService implements INewsletterOrchestrator {


	private final NewsletterRepository repository;
    private final NewsletterBuilderService builder;
    private final NewsLetterReaderService reader;
    private final CustomerRepository customerRepository;
    private final NewsletterService service;
    private static final Logger LOGGER = LoggerFactory.getLogger(NewsletterOrchestratorService.class);

    public NewsletterOrchestratorService(NewsletterBuilderService builder,
                                        NewsLetterReaderService reader,
                                        CustomerRepository customerRepository,
                                        NewsletterRepository repository,
                                        NewsletterService service) {
        this.builder = builder;
        this.reader = reader;
        this.customerRepository = customerRepository;
        this.repository = repository;
        this.service = service;
    }
	
    @Transactional
	@Override
    public void includeMonthlyNewsletter(List<MultipartFile> files, boolean isMatriz) {

        try {
            MultipartFile nfeFile = files.stream()
                    .filter(f -> f.getOriginalFilename().contains("NFe"))
                    .findFirst().orElseThrow(() -> new FileNotFoundException("Arquivo NFe não encontrado"));

            MultipartFile osFile = files.stream()
                    .filter(f -> f.getOriginalFilename().contains("OS"))
                    .findFirst().orElseThrow(() -> new FileNotFoundException("Arquivo OS não encontrado"));

            MultipartFile horasFile = files.stream()
                    .filter(f -> f.getOriginalFilename().contains("Horas"))
                    .findFirst().orElseThrow(() -> new FileNotFoundException("Arquivo Horas não encontrado"));

            MultipartFile pecasFile = files.stream()
                    .filter(f -> f.getOriginalFilename().contains("Pecas"))
                    .findFirst().orElseThrow(() -> new FileNotFoundException("Arquivo Pecas não encontrado"));

            List<NewsletterNFeInfo> nfeInfos = reader.getNfeInfoByExcel(nfeFile.getInputStream());
            List<NewsletterServiceOrders> ordersInfos = reader.getServiceOrdersByExcel(osFile.getInputStream());
            List<NewsletterTechnicalHours> hoursInfos = reader.getTechnicalHoursByExcel(horasFile.getInputStream());
            List<NewsletterExchangedParts> parts = reader.getExchangedPartsByExcel(pecasFile.getInputStream());

            NewsletterData data = new NewsletterData(nfeInfos, ordersInfos, hoursInfos, parts);

            List<Customer> customers = customerRepository.findAll();
            List<Newsletter> newsletters = builder.buildNewsletters(data, customers, isMatriz);

            if (!newsletters.isEmpty()) {
            	newsletters.removeIf(n -> n.getFaturamentoTotal() <= 0);
                repository.saveAll(newsletters);
            }

        }catch (FileNotFoundException e) {
			LOGGER.error(e.getMessage());
		}
        catch (Exception e) {
            LOGGER.error("Ocorreu um erro ao criar as newsletters. Error: " + e.getMessage());
        }
    }

    @Override
    public void includeMonthlyNewsletterByExcel(MultipartFile file) {
            if(file == null || file.isEmpty())
                throw new NewsletterFileNotValidException("Arquivo não encontrado");

        try {
            List<Newsletter> newsletters = reader.getNewsletterInfoByExcel(file.getInputStream());
            if (newsletters == null || newsletters.isEmpty()) {
                throw new NewsletterNullException("Nenhum dado válido encontrado na planilha.");
            }
            repository.saveAll(newsletters);
        } catch (IOException e) {
            throw new NewsletterFileNotValidException("Erro ao ler o arquivo enviado.");
        }
    }

    @Override
	public void executeMonthlyNewsletter() {
		List<Newsletter> newslettersToSend = repository.findAllByStatusIn(List.of(EmailStatus.PENDING, EmailStatus.RETRYING));
		LOGGER.info("Iniciando envios de emails");
		for(Newsletter newsletter: newslettersToSend) {
			try {
				service.sendMailWithInline(newsletter);
				newsletter.setStatus(EmailStatus.SENT);
				LOGGER.info("Email enviado com sucesso!");
			}catch (Exception e) {
				newsletter.setStatus(EmailStatus.ERROR);
				 LOGGER.error("Erro ao enviar newsletter para " + newsletter.getCodigoCliente() + ": " + e.getMessage());
			}finally {
				repository.save(newsletter);
				LOGGER.info("Email Atualizado no banco de dados");
			}
		}
		LOGGER.info("Newsletters enviadas com sucesso!");
	}
}
