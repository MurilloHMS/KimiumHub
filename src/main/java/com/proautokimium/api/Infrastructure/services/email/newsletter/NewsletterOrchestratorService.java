package com.proautokimium.api.Infrastructure.services.email.newsletter;

import java.io.File;
import java.io.FileInputStream;
import java.io.UnsupportedEncodingException;
import java.util.List;
import java.util.Map;

import javax.imageio.stream.FileImageInputStream;

import com.proautokimium.api.Infrastructure.services.pdf.FileNameSanitizerService;
import com.proautokimium.api.domain.entities.Customer;
import com.proautokimium.api.domain.models.Newsletter;
import com.proautokimium.api.domain.models.newsletter.NewsletterExchangedParts;
import com.proautokimium.api.domain.models.newsletter.NewsletterNFeInfo;
import com.proautokimium.api.domain.models.newsletter.NewsletterServiceOrders;
import com.proautokimium.api.domain.models.newsletter.NewsletterTechnicalHours;

import jakarta.mail.MessagingException;
import lombok.experimental.var;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.proautokimium.api.Application.DTOs.email.NewsletterData;
import com.proautokimium.api.Infrastructure.interfaces.email.newsletter.INewsletterOrchestrator;
import com.proautokimium.api.Infrastructure.repositories.CustomerRepository;

@Service
public class NewsletterOrchestratorService implements INewsletterOrchestrator {

    private final NewsletterService newsletterService;

	private final NewsletterBuilderService builder;
	private final NewsLetterReaderService reader;
	private final CustomerRepository customerRepository;
	
	private final File nfeFile;
	private final File osFile;
	private final File horasFile;
	private final File pecasFile;
	
	@Autowired
	public NewsletterOrchestratorService(NewsletterBuilderService builder, NewsLetterReaderService reader, CustomerRepository customerRepository, List<File> files,NewsletterService newsletterService) {
		this.builder = builder;
		this.reader = reader;
		this.customerRepository = customerRepository;
		
		
		this.nfeFile = files.stream().filter(f -> f.getName().contains("NFe")).findFirst().orElseThrow();
		this.osFile = files.stream().filter(f -> f.getName().contains("OS")).findFirst().orElseThrow();
		this.horasFile = files.stream().filter(f -> f.getName().contains("Horas")).findFirst().orElseThrow();
		this.pecasFile = files.stream().filter(f -> f.getName().contains("Pecas")).findFirst().orElseThrow();
		this.newsletterService = newsletterService;
	}
	
	@Override
	public void executeMonthlyNewsletter() {
		
		try {
			List<NewsletterNFeInfo> nfeInfos = reader.getNfeInfoByExcel(new FileInputStream(nfeFile));
			List<NewsletterServiceOrders> ordersInfos = reader.getServiceOrdersByExcel(new FileInputStream(osFile));
			List<NewsletterTechnicalHours> hoursInfos = reader.getTechnicalHoursByExcel(new FileInputStream(horasFile));
			List<NewsletterExchangedParts> parts = reader.getExchangedPartsByExcel(new FileInputStream(pecasFile));
			
			NewsletterData data = new NewsletterData(nfeInfos, ordersInfos, hoursInfos, parts);
			
			List<Customer> customers = customerRepository.findAll();
			
			List<Newsletter> newsletters =  builder.buildNewsletters(data, customers);
			
			newsletters.forEach(n -> {
				try {
					newsletterService.sendMailWithInline(n);
				} catch (UnsupportedEncodingException | MessagingException e) {
					e.printStackTrace();
				}
			});
		}catch (Exception e) {
			e.printStackTrace();
		}
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
