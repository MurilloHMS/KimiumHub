package com.proautokimium.api.controllers;

import com.proautokimium.api.Application.DTOs.client.ClientMeDTO;
import com.proautokimium.api.Application.DTOs.client.ClientUnitDTO;
import com.proautokimium.api.Application.DTOs.email.NewsletterResponseDTO;
import com.proautokimium.api.Infrastructure.converters.NewsletterConverter;
import com.proautokimium.api.Infrastructure.repositories.NewsletterRepository;
import com.proautokimium.api.Infrastructure.services.client.ClientAccessService;
import com.proautokimium.api.domain.entities.Customer;
import com.proautokimium.api.domain.enums.EmailStatus;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("api/client")
@PreAuthorize("hasRole('CLIENTE')")
public class ClientController {

    private final ClientAccessService access;
    private final NewsletterRepository newsletterRepository;
    private final NewsletterConverter converter;

    public ClientController(ClientAccessService access, NewsletterRepository newsletterRepository, NewsletterConverter converter) {
        this.access = access;
        this.newsletterRepository = newsletterRepository;
        this.converter = converter;
    }

    /**
     * Identidade da sessão. O portal chama a cada carga em vez de ler claim do
     * token: o JWT vale 2h e não acompanha mudança de cadastro.
     */
    @GetMapping("/me")
    public ResponseEntity<ClientMeDTO> me() {
        Customer customer = access.currentCustomer();

        List<ClientUnitDTO> unidades = access.visibleUnits().stream()
                .map(unit -> new ClientUnitDTO(unit.getCodParceiro(), unit.getName(),
                        unit.getDocumento(), unit.isMatriz()))
                .toList();

        return ResponseEntity.ok(new ClientMeDTO(customer.getName(), customer.getCodParceiro(),
                customer.getDocumento(), customer.isMatriz(), unidades));
    }

    @GetMapping("/newsletter")
    public ResponseEntity<List<NewsletterResponseDTO>> newsletter(
            @RequestParam(required = false) List<String> units,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {

        List<String> codes = access.allowedCodes(units);

        if (codes.isEmpty()) return ResponseEntity.ok(List.of());

        var newsletters = newsletterRepository
                .findByCodigoClienteInAndStatusAndDataBetweenOrderByDataAsc(codes, EmailStatus.SENT, from, to)
                .stream()
                .map(converter::toDto)
                .toList();

        return ResponseEntity.ok(newsletters);
    }
}