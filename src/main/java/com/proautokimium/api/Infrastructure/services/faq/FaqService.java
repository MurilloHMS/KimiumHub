package com.proautokimium.api.Infrastructure.services.faq;

import com.proautokimium.api.Application.DTOs.faq.FaqCreateDTO;
import com.proautokimium.api.Application.DTOs.faq.FaqPublicResponseDTO;
import com.proautokimium.api.Application.DTOs.faq.FaqResponseDTO;
import com.proautokimium.api.Application.DTOs.faq.FaqUpdateDTO;
import com.proautokimium.api.Infrastructure.converters.FaqConverter;
import com.proautokimium.api.Infrastructure.repositories.FaqRepository;
import com.proautokimium.api.domain.entities.Faq;
import jakarta.transaction.Transactional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
public class FaqService {

    @Autowired
    private FaqRepository repository;

    @Autowired
    private FaqConverter converter;

    public List<FaqResponseDTO> getAll(){
        return repository.findAll().stream().map(converter::toDto).toList();
    }

    public List<FaqPublicResponseDTO> getAllPublic(){
        return repository.findAll().stream().map(converter::toPublicDto).toList();
    }

    @Transactional
    public void create(FaqCreateDTO dto){
        Faq faq = converter.fromCreateDto(dto);
        repository.save(faq);
    }

    @Transactional
    public void update(UUID id,FaqUpdateDTO dto){
        Optional<Faq> faq = repository.findById(id);
        faq.ifPresent(value -> converter.updateFromDto(dto, value));
    }

    @Transactional
    public void setPublished(UUID id){
        repository.findById(id).ifPresent(Faq::publicar);
    }

    @Transactional
    public void setDraft(UUID id){
        repository.findById(id).ifPresent(Faq::rascunho);
    }

    @Transactional
    public void setArchived(UUID id){
        repository.findById(id).ifPresent(Faq::arquivar);
    }
}
