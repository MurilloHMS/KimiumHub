package com.proautokimium.api.Infrastructure.services.product;

import com.proautokimium.api.Application.DTOs.product.ProductWebSiteCreateDTO;
import com.proautokimium.api.Application.DTOs.product.ProductWebSitePublicResponseDTO;
import com.proautokimium.api.Application.DTOs.product.ProductWebSiteResponseDTO;
import com.proautokimium.api.Application.DTOs.product.ProductWebSiteUpdateDTO;
import com.proautokimium.api.Infrastructure.converters.ProductWebSiteConverter;
import com.proautokimium.api.Infrastructure.exceptions.product.ProductNotFoundException;
import com.proautokimium.api.Infrastructure.repositories.ProductWebSiteRepository;
import com.proautokimium.api.Infrastructure.services.storage.ProductImageStorageService;
import com.proautokimium.api.domain.entities.ProductWebsite;
import jakarta.transaction.Transactional;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;
import java.util.UUID;

@Service
public class ProductWebsiteService {

    private final ProductWebSiteRepository repository;
    private final ProductWebSiteConverter converter;
    private final ProductImageStorageService storage;

    public ProductWebsiteService(ProductWebSiteRepository repository, ProductWebSiteConverter converter, ProductImageStorageService storage) {
        this.repository = repository;
        this.converter = converter;
        this.storage = storage;
    }

    @Transactional
    public void create(ProductWebSiteCreateDTO dto, MultipartFile imagem) throws IOException {
        ProductWebsite entity = converter.fromCreateDto(dto);

        if(imagem != null && !imagem.isEmpty()){
            String filename = storage.saveImage(imagem, dto.systemCode());
            entity.setImagem(filename);
        }
        repository.save(entity);
    }

    @Transactional
    public void update(ProductWebSiteUpdateDTO dto, UUID id, MultipartFile imagem) throws IOException {
        ProductWebsite entity = repository.findById(id).orElseThrow(ProductNotFoundException::new);
        converter.updateFromDto(dto, entity);

        if(imagem != null && !imagem.isEmpty()){
            String filename = storage.saveImage(imagem, entity.getSystemCode());
            entity.setImagem(filename);
        }
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

    public List<ProductWebSitePublicResponseDTO> getAllactiveProducts(){
        return repository.findAllByActive(true).stream().map(converter::toPublicDto).toList();
    }
}
