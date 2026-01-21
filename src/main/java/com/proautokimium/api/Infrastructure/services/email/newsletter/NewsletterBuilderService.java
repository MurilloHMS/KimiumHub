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
        // mapeamento
        Map<String, Customer> customersMap =
                customers.stream()
                        .collect(Collectors.toMap(
                                Customer::getCodParceiro,
                                Function.identity()
                        ));

        Map<String, List<NewsletterNFeInfo>> notesPerPartner =
                data.nFeInfos().stream()
                        .collect(Collectors.groupingBy(
                                NewsletterNFeInfo::getPartnerCode
                        ));

        Map<String, List<NewsletterServiceOrders>> ordersPerPartner =
                data.orders().stream()
                        .collect(Collectors.groupingBy(
                                NewsletterServiceOrders::getPartnerCode
                        ));

        Map<String, List<NewsletterExchangedParts>> partsPerPartner =
                data.parts().stream()
                        .collect(Collectors.groupingBy(
                                NewsletterExchangedParts::getPartnerCode
                        ));

        // Horas SEM mau uso
        Map<String, List<NewsletterTechnicalHours>> hoursWithoutMinuse =
                data.hours().stream()
                        .filter(h -> !h.isMinuse())
                        .collect(Collectors.groupingBy(
                                NewsletterTechnicalHours::getPartnerCode
                        ));

        // Horas COM mau uso
        Map<String, List<NewsletterTechnicalHours>> hoursWithMinuse =
                data.hours().stream()
                        .filter(NewsletterTechnicalHours::isMinuse)
                        .collect(Collectors.groupingBy(
                                NewsletterTechnicalHours::getPartnerCode
                        ));
		
		if(isMatriz) {
			Map<String, String> partnerToMatriz = customers.stream()
				    .collect(Collectors.toMap(
				        Customer::getCodParceiro,
				        c -> c.getCodigoMatriz() == null || c.getCodigoMatriz().isBlank()
                            ? c.getCodParceiro()
                            : c.getCodigoMatriz()
				    ));


			// regroup to matriz
			notesPerPartner = regroup(notesPerPartner, partnerToMatriz);
            ordersPerPartner = regroup(ordersPerPartner, partnerToMatriz);
            partsPerPartner = regroup(partsPerPartner, partnerToMatriz);
            hoursWithoutMinuse = regroup(hoursWithoutMinuse, partnerToMatriz);
            hoursWithMinuse = regroup(hoursWithMinuse, partnerToMatriz);

		}
		
		Set<String> allPartners = new HashSet<>(notesPerPartner.keySet());
		
		List<Newsletter> newsletters = new ArrayList<>();


        // construção da newsletter
		for(String code : allPartners) {
			List<NewsletterNFeInfo> notes = notesPerPartner.getOrDefault(code, List.of());

            if(notes.isEmpty())	continue;
			
			NewsletterNFeInfo firstWithDate = notes.stream().filter(x -> x.getDate() != null).findFirst().orElse(null);

            if(firstWithDate == null) continue;

			Customer partner = customersMap.get(code);
			
			Newsletter newsletter = new Newsletter();

            // Dados Básicos
			newsletter.setCodigoCliente(code);
			newsletter.setNomeDoCliente(partner != null ? partner.getName() : firstWithDate.getPartnerName());
            newsletter.setData(firstWithDate.getDate());
            newsletter.setMes(firstWithDate.getDate().getMonth().getDisplayName(TextStyle.FULL, Locale.of("pt", "BR")));

            // Notas
			newsletter.setQuantidadeNotasEmitidas((int) notes.stream().map(NewsletterNFeInfo::getNfeNumber).distinct().count());
			newsletter.setFaturamentoTotal(notes.stream().mapToDouble(NewsletterNFeInfo::getValueWithTaxes).sum());
			newsletter.setQuantidadeDeProdutos((int) notes.stream().map(NewsletterNFeInfo::getProductCode).distinct().count());
			newsletter.setQuantidadeDeLitros(notes.stream().mapToDouble(NewsletterNFeInfo::getQuantity).sum());

            // Produto destaque

            notes.stream()
                    .collect(Collectors.groupingBy(
                            NewsletterNFeInfo::getProductName,
                            Collectors.summingDouble(NewsletterNFeInfo::getQuantity)
                    ))
                    .entrySet()
                    .stream()
                    .max(Map.Entry.comparingByValue())
                    .ifPresent(e -> newsletter.setProdutoEmDestaque(e.getKey()));

            // Ordens
			
			List<NewsletterServiceOrders> orders = ordersPerPartner.getOrDefault(code, List.of());
			newsletter.setQuantidadeDeVisitas(orders.size());
			
			orders.stream()
                    .mapToInt(NewsletterServiceOrders::getDaysOfWeek)
                    .average()
                    .ifPresent(avg -> newsletter.setMediaDiasAtendimento((int) avg));

            // Pecas

            List<NewsletterTechnicalHours> normalHours = hoursWithoutMinuse.getOrDefault(code, List.of());

            // Horas Normais
            newsletter.setValorTotalDeHoras(
                    normalHours.stream()
                            .mapToDouble(NewsletterTechnicalHours::getTimePerPartner)
                            .sum()
            );

            newsletter.setValorTotalCobradoHoras(
                    normalHours.stream()
                            .mapToDouble(NewsletterTechnicalHours::getTotalValuePerPartner)
                            .sum()
            );

            // Horas Mau Uso

            List<NewsletterTechnicalHours> minuseHours = hoursWithMinuse.getOrDefault(code, List.of());

            newsletter.setValorTotalDeHorasMauUso(
                    minuseHours.stream()
                            .mapToDouble(NewsletterTechnicalHours::getMinuseHour)
                            .sum()
            );

            newsletter.setValorTotalCobradoHorasMauUso(
                    minuseHours.stream()
                            .mapToDouble(NewsletterTechnicalHours::getMinuseValue)
                            .sum()
            );

            newsletter.setMauUso(!minuseHours.isEmpty());

            // Email

            newsletter.setEmailCliente(
                    partner != null && partner.getEmail() != null
                        ? partner.getEmail().getAddress()
                        : null
            );

            newsletter.setStatus(EmailStatus.PENDING);
            newsletters.add(newsletter);
		}
		
		return newsletters;
	}

    private <T> Map<String, List<T>> regroup(
            Map<String, List<T>> source,
            Map<String, String> partnerToMatriz) {

        return source.entrySet().stream()
                .collect(Collectors.groupingBy(
                        e -> partnerToMatriz.getOrDefault(e.getKey(), e.getKey()),
                        Collectors.flatMapping(
                                e -> e.getValue().stream(),
                                Collectors.toList()
                        )
                ));
    }

}
