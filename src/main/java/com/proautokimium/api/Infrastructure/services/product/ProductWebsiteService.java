package com.proautokimium.api.Infrastructure.services.product;

import com.proautokimium.api.Application.DTOs.product.ProductWebSiteCreateDTO;
import com.proautokimium.api.Application.DTOs.product.ProductWebSiteResponseDTO;
import com.proautokimium.api.Application.DTOs.product.ProductWebSiteUpdateDTO;
import com.proautokimium.api.Infrastructure.converters.ProductWebSiteConverter;
import com.proautokimium.api.Infrastructure.exceptions.product.ProductNotFoundException;
import com.proautokimium.api.Infrastructure.repositories.ProductWebSiteRepository;
import com.proautokimium.api.domain.entities.ProductWebsite;
import jakarta.transaction.Transactional;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Service
public class ProductWebsiteService {

    private final ProductWebSiteRepository repository;
    private final ProductWebSiteConverter converter;

    public ProductWebsiteService(ProductWebSiteRepository repository, ProductWebSiteConverter converter) {
        this.repository = repository;
        this.converter = converter;
    }

    @Transactional
    public void create(ProductWebSiteCreateDTO dto){
        ProductWebsite entity = converter.fromCreateDto(dto);
        repository.save(entity);
    }

    @Transactional
    public void update(ProductWebSiteUpdateDTO dto, UUID id){
        ProductWebsite entity = repository.findById(id).orElseThrow(ProductNotFoundException::new);
        converter.updateFromDto(dto, entity);
        repository.save(entity);
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

    public List<ProductWebSiteResponseDTO> getAll(){
        return repository.findAll().stream().map(converter::toDto).toList();
    }

    public List<ProductWebSiteResponseDTO> getAllactiveProducts(){
        return repository.findAllByActive(true).stream().map(converter::toDto).toList();
    }
}
