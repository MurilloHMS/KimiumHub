package com.proautokimium.api.Infrastructure.services.email.newsletter;

import java.io.FileInputStream;
import java.util.List;

import com.proautokimium.api.domain.entities.Customer;
import com.proautokimium.api.domain.models.Newsletter;
import com.proautokimium.api.domain.models.newsletter.NewsletterExchangedParts;
import com.proautokimium.api.domain.models.newsletter.NewsletterNFeInfo;
import com.proautokimium.api.domain.models.newsletter.NewsletterServiceOrders;
import com.proautokimium.api.domain.models.newsletter.NewsletterTechnicalHours;

import org.springframework.beans.factory.annotation.Autowired;
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

    @Autowired
    public NewsletterOrchestratorService(NewsletterBuilderService builder,
                                        NewsLetterReaderService reader,
                                        CustomerRepository customerRepository,
                                        NewsletterRepository repository) {
        this.builder = builder;
        this.reader = reader;
        this.customerRepository = customerRepository;
        this.repository = repository;
    }
	
	@Override
    public void includeMonthlyNewsletter(List<MultipartFile> files) {

        try {
            MultipartFile nfeFile = files.stream()
                    .filter(f -> f.getOriginalFilename().contains("NFe"))
                    .findFirst().orElseThrow(() -> new RuntimeException("Arquivo NFe não encontrado"));

            MultipartFile osFile = files.stream()
                    .filter(f -> f.getOriginalFilename().contains("OS"))
                    .findFirst().orElseThrow(() -> new RuntimeException("Arquivo OS não encontrado"));

            MultipartFile horasFile = files.stream()
                    .filter(f -> f.getOriginalFilename().contains("Horas"))
                    .findFirst().orElseThrow(() -> new RuntimeException("Arquivo Horas não encontrado"));

            MultipartFile pecasFile = files.stream()
                    .filter(f -> f.getOriginalFilename().contains("Pecas"))
                    .findFirst().orElseThrow(() -> new RuntimeException("Arquivo Pecas não encontrado"));

            List<NewsletterNFeInfo> nfeInfos = reader.getNfeInfoByExcel(nfeFile.getInputStream());
            List<NewsletterServiceOrders> ordersInfos = reader.getServiceOrdersByExcel(osFile.getInputStream());
            List<NewsletterTechnicalHours> hoursInfos = reader.getTechnicalHoursByExcel(horasFile.getInputStream());
            List<NewsletterExchangedParts> parts = reader.getExchangedPartsByExcel(pecasFile.getInputStream());

            NewsletterData data = new NewsletterData(nfeInfos, ordersInfos, hoursInfos, parts);

            List<Customer> customers = customerRepository.findAll();
            List<Newsletter> newsletters = builder.buildNewsletters(data, customers);

            if (!newsletters.isEmpty()) {
                repository.saveAll(newsletters);
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
	
	@Override 
	public void executeMonthlyNewsletter() {
		
	}
	
	private FileInputStream findFile(List<FileInputStream> streams, String keyword) {
		return streams.stream()
				.filter(fis -> {
					try {
						String name = fis.getFD().toString();
						return name.contains(keyword);
					}catch (Exception e) {
						return false;
					}
				})
				.findFirst()
				.orElseThrow(() -> new RuntimeException("Arquivo " + keyword + "não encontrado"));
	}
}
