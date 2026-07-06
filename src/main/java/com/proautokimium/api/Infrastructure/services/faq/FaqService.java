package com.proautokimium.api.Infrastructure.services.faq;

import com.proautokimium.api.Application.DTOs.faq.FaqCreateDTO;
import com.proautokimium.api.Application.DTOs.faq.FaqPublicResponseDTO;
import com.proautokimium.api.Application.DTOs.faq.FaqResponseDTO;
import com.proautokimium.api.Application.DTOs.faq.FaqUpdateDTO;
import com.proautokimium.api.Infrastructure.converters.FaqConverter;
import com.proautokimium.api.Infrastructure.repositories.FaqRepository;
import com.proautokimium.api.domain.entities.Faq;
import com.proautokimium.api.domain.enums.StatusPostagem;
import jakarta.transaction.Transactional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Serviço para gerenciar o registro de um FAQ.
 */
@Service
public class FaqService {

    @Autowired
    private FaqRepository repository;

    @Autowired
    private FaqConverter converter;

    /**
     * Obtém a lista de FAQ's cadastrados
     * @return Lista de FaqResponseDTO
     */
    public List<FaqResponseDTO> getAll(){
        return repository.findAll().stream().map(converter::toDto).toList();
    }

    /**
     * Obtém a lista de FAQ's publicados
     * @return Lista de FaqPublicResponseDTO
     */
    public List<FaqPublicResponseDTO> getAllPublic(){
        return repository.findAllByStatus(StatusPostagem.PUBLICADO).stream().map(converter::toPublicDto).toList();
    }

    /**
     * Registra um FAQ
     * @param dto DTO para cadastro dos FAQ's
     */
    @Transactional
    public void create(FaqCreateDTO dto){
        Faq faq = converter.fromCreateDto(dto);
        repository.save(faq);
    }

    /**
     * Atualiza um FAQ
     * @param dto DTO para atualização dos FAQ's
     */
    @Transactional
    public void update(UUID id,FaqUpdateDTO dto){
        Optional<Faq> faq = repository.findById(id);
        faq.ifPresent(value -> converter.updateFromDto(dto, value));
    }

    /**
     * Altera o status do FAQ para PUBLICADO
     * @param id UUID do FAQ
     */
    @Transactional
    public void setPublished(UUID id){
        repository.findById(id).ifPresent(Faq::publicar);
    }

    /**
     * Altera o status do FAQ para RASCUNHO
     * @param id UUID do FAQ
     */
    @Transactional
    public void setDraft(UUID id){
        repository.findById(id).ifPresent(Faq::rascunho);
    }

    /**
     * Altera o status do FAQ para ARQUIVADO
     * @param id UUID do FAQ
     */
    @Transactional
    public void setArchived(UUID id){
        repository.findById(id).ifPresent(Faq::arquivar);
    }
}
