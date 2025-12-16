package com.proautokimium.api.Infrastructure.services.email.newsletter;

import java.time.format.TextStyle;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import com.proautokimium.api.Application.DTOs.email.NewsletterData;
import com.proautokimium.api.Infrastructure.interfaces.email.newsletter.INewsletterBuilder;
import com.proautokimium.api.domain.entities.Customer;
import com.proautokimium.api.domain.entities.Newsletter;
import com.proautokimium.api.domain.enums.EmailStatus;
import com.proautokimium.api.domain.models.newsletter.NewsletterExchangedParts;
import com.proautokimium.api.domain.models.newsletter.NewsletterNFeInfo;
import com.proautokimium.api.domain.models.newsletter.NewsletterServiceOrders;
import com.proautokimium.api.domain.models.newsletter.NewsletterTechnicalHours;

@Service
public class NewsletterBuilderService implements INewsletterBuilder {

	@Override
	public List<Newsletter> buildNewsletters(NewsletterData data, List<Customer> customers, boolean isMatriz) {
		
		// mappings
		Map<String, Customer> customersMap;
		Map<String, List<NewsletterNFeInfo>> notesPerPartnersMap;
		Map<String, List<NewsletterServiceOrders>> ordersPerPartnersMap;
		Map<String, List<NewsletterTechnicalHours>> hoursPerPartnersWithoutMinuse;
		Map<String, List<NewsletterTechnicalHours>> hoursPerPartnersWithMinuse;
		Map<String, List<NewsletterExchangedParts>> partsPerPartnersMap;


		// customer map
		customersMap = customers.stream()
			    .collect(Collectors.toMap(Customer::getCodParceiro, Function.identity()));


		// ALWAYS group data by partner code
		notesPerPartnersMap = data.nFeInfos().stream()
		.collect(Collectors.groupingBy(NewsletterNFeInfo::getPartnerCode));


		ordersPerPartnersMap = data.orders().stream()
		.collect(Collectors.groupingBy(NewsletterServiceOrders::getPartnerCode));


		hoursPerPartnersWithoutMinuse = data.hours().stream()
		.filter(n -> !n.isMinuse())
		.collect(Collectors.groupingBy(NewsletterTechnicalHours::getPartnerCode));


		hoursPerPartnersWithMinuse = data.hours().stream()
		.filter(NewsletterTechnicalHours::isMinuse)
		.collect(Collectors.groupingBy(NewsletterTechnicalHours::getPartnerCode));


		partsPerPartnersMap = data.parts().stream()
		.collect(Collectors.groupingBy(NewsletterExchangedParts::getPartnerCode));
		
		
		
		if(isMatriz) {
			Map<String, String> partnerToMatriz = customers.stream()
				    .collect(Collectors.toMap(
				        Customer::getCodParceiro,
				        c -> {
				            String matriz = c.getCodigoMatriz();
				            return (matriz == null || matriz.isBlank()) ? c.getCodParceiro() : matriz;
				        }
				    ));


			// regroup to matriz
			notesPerPartnersMap = notesPerPartnersMap.entrySet().stream()
			    .collect(Collectors.groupingBy(
			        e -> partnerToMatriz.getOrDefault(e.getKey(), e.getKey()),
			        Collectors.flatMapping(e -> e.getValue().stream(), Collectors.toList())
			    ));

			ordersPerPartnersMap = ordersPerPartnersMap.entrySet().stream()
			    .collect(Collectors.groupingBy(
			        e -> partnerToMatriz.getOrDefault(e.getKey(), e.getKey()),
			        Collectors.flatMapping(e -> e.getValue().stream(), Collectors.toList())
			    ));

			hoursPerPartnersWithoutMinuse = hoursPerPartnersWithoutMinuse.entrySet().stream()
			    .collect(Collectors.groupingBy(
			        e -> partnerToMatriz.getOrDefault(e.getKey(), e.getKey()),
			        Collectors.flatMapping(e -> e.getValue().stream(), Collectors.toList())
			    ));

			hoursPerPartnersWithMinuse = hoursPerPartnersWithMinuse.entrySet().stream()
			    .collect(Collectors.groupingBy(
			        e -> partnerToMatriz.getOrDefault(e.getKey(), e.getKey()),
			        Collectors.flatMapping(e -> e.getValue().stream(), Collectors.toList())
			    ));

			partsPerPartnersMap = partsPerPartnersMap.entrySet().stream()
			    .collect(Collectors.groupingBy(
			        e -> partnerToMatriz.getOrDefault(e.getKey(), e.getKey()),
			        Collectors.flatMapping(e -> e.getValue().stream(), Collectors.toList())
			    ));

		}
		
		Set<String> allPartnerSet = new HashSet<>();
		allPartnerSet.addAll(notesPerPartnersMap.keySet());
		
		List<Newsletter> newsletters = new ArrayList<>();
		
		for(String code : allPartnerSet) {
			List<NewsletterNFeInfo> notes = notesPerPartnersMap.getOrDefault(code, Collections.emptyList());
			if(notes.isEmpty())
				continue;
			
			NewsletterNFeInfo firstWithDate = notes.stream().filter(x -> x.getDate() != null).findFirst().orElse(null);
			Customer partner = customersMap.get(code);
			
			Newsletter newsletter = new Newsletter();
			newsletter.setCodigoCliente(code);
			newsletter.setNomeDoCliente(partner != null ? partner.getName() : notes.get(0).getPartnerName());
			newsletter.setQuantidadeNotasEmitidas((int) notes.stream().map(n -> Integer.parseInt(n.getNfeNumber())).distinct().count());
			newsletter.setFaturamentoTotal(notes.stream().mapToDouble(NewsletterNFeInfo::getValueWithTaxes).sum());
			newsletter.setQuantidadeDeProdutos((int) notes.stream().map(n -> Integer.parseInt(n.getProductCode())).distinct().count());
			newsletter.setQuantidadeDeLitros(notes.stream().mapToDouble(NewsletterNFeInfo::getQuantity).sum());
			newsletter.setData(firstWithDate.getDate());
			
			String month = firstWithDate.getDate().getMonth().getDisplayName(TextStyle.FULL, Locale.of("pt", "BR"));
			newsletter.setMes(month);
			
			Map<String, Double> produtosVendidos = notes.stream().collect(Collectors.groupingBy(NewsletterNFeInfo::getProductName, Collectors.summingDouble(NewsletterNFeInfo::getQuantity)));
			Optional<Map.Entry<String, Double>> produtoMaisVendido = produtosVendidos.entrySet().stream().max(Map.Entry.comparingByValue());
			
			produtoMaisVendido.ifPresent(entry -> newsletter.setProdutoEmDestaque(entry.getKey()));
			
			
			List<NewsletterServiceOrders> orders = ordersPerPartnersMap.getOrDefault(code, Collections.emptyList());
			newsletter.setQuantidadeDeVisitas(orders.size());
			
			if(!orders.isEmpty()) {
				int daysAverage = (int) orders.stream().mapToInt(NewsletterServiceOrders::getDaysOfWeek).average().orElse(0);
				newsletter.setMediaDiasAtendimento(daysAverage);
			}
			
			List<NewsletterExchangedParts> parts = partsPerPartnersMap.getOrDefault(code, Collections.emptyList());
			double partsValue = parts.stream().mapToDouble(NewsletterExchangedParts::getTotalCost).sum();
			newsletter.setValorDePecasTrocadas(partsValue);
			
			// total hours without misuse
			List<NewsletterTechnicalHours> hours = hoursPerPartnersWithoutMinuse.getOrDefault(code, Collections.emptyList());
			double totalHours = hours.isEmpty() ? 0 : hours.get(0).getTimePerPartner();
			newsletter.setValorTotalDeHoras(totalHours);
			
			double totalHoursValue = hours.isEmpty() ? 0 : hours.get(0).getTotalValuePerPartner();
			newsletter.setValorTotalCobradoHoras(totalHoursValue);
			
			// total hours with misuse
			List<NewsletterTechnicalHours> hoursWithMinuse = hoursPerPartnersWithMinuse.getOrDefault(code, Collections.emptyList());
			double totalHoursWithMinuse = hoursWithMinuse.isEmpty() ? 0 : hoursWithMinuse.get(0).getMinuseHour();
			newsletter.setValorTotalDeHorasMauUso(totalHoursWithMinuse);
			
			double totalHoursWithMinuseValue = hoursWithMinuse.isEmpty() ? 0 : hoursWithMinuse.get(0).getMinuseValue();
			newsletter.setValorTotalCobradoHorasMauUso(totalHoursWithMinuseValue);
			
			newsletter.setMauUso(hoursWithMinuse.isEmpty() ? false : true);
			
			newsletter.setEmailCliente(partner != null ? partner.getEmail().getAddress() : null);
			
			newsletter.setStatus(EmailStatus.PENDING);
			
			newsletters.add(newsletter);
		}
		
		return newsletters;
	}

}
