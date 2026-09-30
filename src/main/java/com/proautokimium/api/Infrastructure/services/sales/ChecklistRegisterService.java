package com.proautokimium.api.Infrastructure.services.sales;

import com.proautokimium.api.Application.DTOs.sales.ChecklistCatalogDTO;
import com.proautokimium.api.Application.DTOs.sales.ChecklistRegisterDTO;
import com.proautokimium.api.Infrastructure.exceptions.sales.ChecklistRegisterException;
import com.proautokimium.api.Infrastructure.repositories.sales.ChecklistComodatoItemRepository;
import com.proautokimium.api.Infrastructure.repositories.sales.ChecklistVisualItemRepository;
import com.proautokimium.api.domain.entities.sales.ChecklistComodatoItem;
import com.proautokimium.api.domain.entities.sales.ChecklistVisualItem;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Os cadastros do checklist: itens de comunicação visual e equipamentos de
 * comodato. Quem mantém é a Controladoria.
 *
 * Nada é apagado: item que sai é desativado, porque checklists antigos o citam
 * e o PDF deles precisa continuar legível.
 */
@Service
public class ChecklistRegisterService {

    /** Os grupos do Sankhya de onde o comodato é escolhido (medidos em 2026-09-29). */
    static final Set<Long> COMODATO_GROUPS = Set.of(800002000L, 800003000L, 600001000L);

    private final ChecklistVisualItemRepository visualRepository;
    private final ChecklistComodatoItemRepository comodatoRepository;
    private final ChecklistCatalogService catalogService;

    public ChecklistRegisterService(ChecklistVisualItemRepository visualRepository,
                                    ChecklistComodatoItemRepository comodatoRepository,
                                    ChecklistCatalogService catalogService) {
        this.visualRepository = visualRepository;
        this.comodatoRepository = comodatoRepository;
        this.catalogService = catalogService;
    }

    // ── Comunicação visual ───────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public List<ChecklistRegisterDTO.VisualItem> visualItems() {
        return visualRepository.findAllByOrderBySortOrderAscNameAsc().stream().map(ChecklistRegisterService::visual).toList();
    }

    @Transactional
    public ChecklistRegisterDTO.VisualItem createVisualItem(ChecklistRegisterDTO.VisualItemRequest request) {
        if (visualRepository.existsByNameIgnoreCase(request.name().strip())) {
            throw new ChecklistRegisterException("Já existe um item com esse nome.", HttpStatus.CONFLICT);
        }
        return visual(visualRepository.save(new ChecklistVisualItem(request.name(), request.sortOrder())));
    }

    @Transactional
    public ChecklistRegisterDTO.VisualItem updateVisualItem(UUID id, ChecklistRegisterDTO.VisualItemRequest request) {
        ChecklistVisualItem item = visualRepository.findById(id).orElseThrow(ChecklistRegisterService::notFound);
        boolean renamed = !item.getName().equalsIgnoreCase(request.name().strip());
        if (renamed && visualRepository.existsByNameIgnoreCase(request.name().strip())) {
            throw new ChecklistRegisterException("Já existe um item com esse nome.", HttpStatus.CONFLICT);
        }
        item.update(request.name(), request.sortOrder(), request.active());
        return visual(visualRepository.save(item));
    }

    // ── Comodato ─────────────────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public List<ChecklistRegisterDTO.ComodatoItem> comodatoItems() {
        Map<Integer, String> names = erpNames();
        return comodatoRepository.findAllByOrderBySortOrderAsc().stream()
                .map(c -> new ChecklistRegisterDTO.ComodatoItem(c.getId(), c.getProductCode(),
                        names.get(c.getProductCode()), c.getPopularName(), c.getSortOrder(), c.isActive()))
                .toList();
    }

    @Transactional
    public ChecklistRegisterDTO.ComodatoItem createComodatoItem(ChecklistRegisterDTO.ComodatoItemRequest request) {
        if (comodatoRepository.existsByProductCode(request.productCode())) {
            throw new ChecklistRegisterException("Esse produto já está na lista de comodato.", HttpStatus.CONFLICT);
        }
        String erpName = erpNames().get(request.productCode());
        if (erpName == null) {
            throw new ChecklistRegisterException("Produto " + request.productCode()
                    + " não encontrado entre os produtos ativos do Sankhya.", HttpStatus.BAD_REQUEST);
        }
        ChecklistComodatoItem saved = comodatoRepository.save(
                new ChecklistComodatoItem(request.productCode(), request.popularName(), request.sortOrder()));
        return new ChecklistRegisterDTO.ComodatoItem(saved.getId(), saved.getProductCode(), erpName,
                saved.getPopularName(), saved.getSortOrder(), saved.isActive());
    }

    @Transactional
    public ChecklistRegisterDTO.ComodatoItem updateComodatoItem(UUID id, ChecklistRegisterDTO.ComodatoItemRequest request) {
        ChecklistComodatoItem item = comodatoRepository.findById(id).orElseThrow(ChecklistRegisterService::notFound);
        item.update(request.popularName(), request.sortOrder(), request.active());
        ChecklistComodatoItem saved = comodatoRepository.save(item);
        return new ChecklistRegisterDTO.ComodatoItem(saved.getId(), saved.getProductCode(),
                erpNames().get(saved.getProductCode()), saved.getPopularName(), saved.getSortOrder(), saved.isActive());
    }

    /** Os produtos dos grupos de comodato, marcando os que já estão na lista. */
    @Transactional(readOnly = true)
    public List<ChecklistRegisterDTO.ComodatoCandidate> comodatoCandidates() {
        Set<Integer> chosen = comodatoRepository.findAll().stream()
                .map(ChecklistComodatoItem::getProductCode).collect(Collectors.toSet());
        return catalogService.products().stream()
                .filter(p -> COMODATO_GROUPS.contains(p.group()))
                .sorted(Comparator.comparing(ChecklistCatalogDTO.Product::name))
                .map(p -> new ChecklistRegisterDTO.ComodatoCandidate(p.code(), p.name(), p.group(),
                        chosen.contains(p.code())))
                .toList();
    }

    private Map<Integer, String> erpNames() {
        return catalogService.products().stream()
                .collect(Collectors.toMap(ChecklistCatalogDTO.Product::code, ChecklistCatalogDTO.Product::name,
                        (a, b) -> a));
    }

    private static ChecklistRegisterDTO.VisualItem visual(ChecklistVisualItem v) {
        return new ChecklistRegisterDTO.VisualItem(v.getId(), v.getName(), v.getSortOrder(), v.isActive());
    }

    private static ChecklistRegisterException notFound() {
        return new ChecklistRegisterException("Item não encontrado.", HttpStatus.NOT_FOUND);
    }
}
