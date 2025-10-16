package com.proautokimium.api.Infrastructure.services.email.newsletter;

import java.time.LocalTime;
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

import org.apache.fontbox.ttf.table.common.CoverageTableFormat1;
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

	@SuppressWarnings("unused")
	@Override
	public List<Newsletter> buildNewsletters(NewsletterData data, List<Customer> customers) {
		
		Map<String, Customer> customersMap = 
				customers.stream().collect(Collectors.toMap(Customer::getCodParceiro, Function.identity()));
		
		Map<String, List<NewsletterNFeInfo>> notesPerPartnersMap = 
				data.nFeInfos().stream().collect(Collectors.groupingBy(NewsletterNFeInfo::getPartnerCode));
		
		Map<String, List<NewsletterServiceOrders>> ordersPerPartnersMap = 
				data.orders().stream().collect(Collectors.groupingBy(NewsletterServiceOrders::getPartnerCode));
		
		Map<String, List<NewsletterTechnicalHours>> hoursPerPartnersMap = 
				data.hours().stream().collect(Collectors.groupingBy(NewsletterTechnicalHours::getPartnerCode));
		
		Map<String, List<NewsletterExchangedParts>> partsPerPartnersMap = 
				data.parts().stream().collect(Collectors.groupingBy(NewsletterExchangedParts::getPartnerCode));
		
		Set<String> allPartnerSet = new HashSet<>();
		allPartnerSet.addAll(notesPerPartnersMap.keySet());
//		allPartnerSet.addAll(ordersPerPartnersMap.keySet());
//		allPartnerSet.addAll(hoursPerPartnersMap.keySet());
//		allPartnerSet.addAll(partsPerPartnersMap.keySet());
		
		List<Newsletter> newsletters = new ArrayList<>();
		
		for(String code : allPartnerSet) {
			List<NewsletterNFeInfo> notes = notesPerPartnersMap.getOrDefault(code, Collections.emptyList());
			if(notes.isEmpty())
				continue;
			
			NewsletterNFeInfo firstWithDate = notes.stream().filter(x -> x.getDate() != null).findFirst().orElse(null);
			
			Newsletter newsletter = new Newsletter();
			newsletter.setCodigoCliente(code);
			newsletter.setNomeDoCliente(notes.get(0).getPartnerName());
			newsletter.setQuantidadeNotasEmitidas(notes.size());
			newsletter.setFaturamentoTotal(notes.stream().mapToDouble(NewsletterNFeInfo::getValueWithTaxes).sum());
			newsletter.setQuantidadeDeProdutos(notes.size());
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
			
			List<NewsletterTechnicalHours> hours = hoursPerPartnersMap.getOrDefault(code, Collections.emptyList());
			double totalHours = hours.isEmpty() ? 0 : hours.get(0).getTimePerPartner();
			newsletter.setValorTotalDeHoras(totalHours);
			
			double totalHoursValue = hours.isEmpty() ? 0 : hours.get(0).getTotalValuePerPartner();
			newsletter.setValorTotalCobradoHoras(totalHoursValue);
			
			Customer partner = customersMap.get(code);
			
			newsletter.setEmailCliente(partner != null ? partner.getEmail().getAddress() : "");
			
			newsletter.setStatus(EmailStatus.PENDING);
			
			newsletters.add(newsletter);
		}
		
		return newsletters;
	}

}
