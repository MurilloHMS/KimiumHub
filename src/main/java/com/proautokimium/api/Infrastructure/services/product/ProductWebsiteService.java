package com.proautokimium.api.Infrastructure.services.product;

import com.proautokimium.api.Application.DTOs.product.ProductWebSiteCreateDTO;
import com.proautokimium.api.Application.DTOs.product.ProductWebSitePublicResponseDTO;
import com.proautokimium.api.Application.DTOs.product.ProductWebSiteResponseDTO;
import com.proautokimium.api.Application.DTOs.product.ProductWebSiteUpdateDTO;
import com.proautokimium.api.Infrastructure.converters.ProductWebSiteConverter;
import com.proautokimium.api.Infrastructure.exceptions.product.ProductNotFoundException;
import com.proautokimium.api.Infrastructure.repositories.EquipmentGuideRepository;
import com.proautokimium.api.Infrastructure.repositories.ProductWebSiteRepository;
import com.proautokimium.api.Infrastructure.services.storage.ProductImageStorageService;
import com.proautokimium.api.domain.entities.EquipmentGuide;
import com.proautokimium.api.domain.entities.ProductWebsite;
import jakarta.persistence.EntityNotFoundException;
import jakarta.transaction.Transactional;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
public class ProductWebsiteService {

    private final ProductWebSiteRepository repository;
    private final ProductWebSiteConverter converter;
    private final ProductImageStorageService storage;
    private final EquipmentGuideRepository equipmentRepository;

    public ProductWebsiteService(ProductWebSiteRepository repository, ProductWebSiteConverter converter,
                                 ProductImageStorageService storage, EquipmentGuideRepository equipmentRepository) {
        this.repository = repository;
        this.converter = converter;
        this.storage = storage;
        this.equipmentRepository = equipmentRepository;
    }

    @Transactional
    public void create(ProductWebSiteCreateDTO dto, MultipartFile imagem) throws IOException {
        ProductWebsite entity = converter.fromCreateDto(dto);
        applyEquipment(entity, dto.equipmentId());

        if(imagem != null && !imagem.isEmpty()){
            String filename = storage.save(imagem, dto.systemCode());
            entity.setImagem(filename);
        }
        repository.save(entity);
    }

    @Transactional
    public void update(ProductWebSiteUpdateDTO dto, UUID id, MultipartFile imagem) throws IOException {
        ProductWebsite entity = repository.findById(id).orElseThrow(ProductNotFoundException::new);
        converter.updateFromDto(dto, entity);
        applyEquipment(entity, dto.equipmentId());

        if(imagem != null && !imagem.isEmpty()){
            String filename = storage.save(imagem, entity.getSystemCode());
            entity.setImagem(filename);
        }
        repository.save(entity);
    }

    /** Define o (único) equipamento do produto. null = remove o vínculo. */
    private void applyEquipment(ProductWebsite entity, UUID equipmentId) {
        List<EquipmentGuide> list = entity.getEquipmentGuides();
        if (list == null) {
            list = new ArrayList<>();
            entity.setEquipmentGuides(list);
        }
        list.clear();
        if (equipmentId != null) {
            EquipmentGuide eq = equipmentRepository.findById(equipmentId)
                    .orElseThrow(() -> new EntityNotFoundException("Equipamento não encontrado: " + equipmentId));
            list.add(eq);
        }
    }

    @Transactional
    public void delete(UUID id){
        if(repository.existsById(id)) repository.deleteById(id);
    }

    @Transactional
    public void hide(UUID id){
        ProductWebsite entity = repository.findById(id).orElseThrow(ProductNotFoundException::new);
        entity.setActive(false);
        repository.save(entity);
    }

    @Transactional
    public void unhide(UUID id){
        ProductWebsite entity = repository.findById(id).orElseThrow(ProductNotFoundException::new);
        entity.setActive(true);
        repository.save(entity);
    }

    @Transactional
    public List<ProductWebSiteResponseDTO> getAll(){
        return repository.findAll().stream().map(converter::toDto).toList();
    }

    public List<ProductWebSitePublicResponseDTO> getAllactiveProducts(){
        return repository.findAllByActive(true).stream().map(converter::toPublicDto).toList();
    }
}
