package com.proautokimium.api.Infrastructure.services.guide;

import com.proautokimium.api.Application.DTOs.product.equipment.ProductEquipmentCreateDTO;
import com.proautokimium.api.Application.DTOs.product.equipment.ProductEquipmentUpdateDTO;
import com.proautokimium.api.Infrastructure.repositories.EquipmentGuideRepository;
import com.proautokimium.api.Infrastructure.services.storage.EquipmentImageStorageService;
import com.proautokimium.api.domain.entities.EquipmentGuide;
import jakarta.transaction.Transactional;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;
import java.util.UUID;

/**
 * Serviço de gerenciamento de {@link EquipmentGuide}.
 *
 * <p>
 * Responsável pelas operações CRUD e pelo upload de imagens
 * utilizando o {@link EquipmentImageStorageService}.
 * </p>
 */
@Service
public class EquipmentGuideService {

    private final EquipmentGuideRepository repository;
    private final EquipmentImageStorageService storage;

    public EquipmentGuideService(
            EquipmentGuideRepository repository,
            EquipmentImageStorageService storage
    ) {
        this.repository = repository;
        this.storage    = storage;
    }

    /**
     * Lista todos os equipamentos cadastrados.
     */
    public List<EquipmentGuide> getAll() {
        return repository.findAll();
    }

    /**
     * Busca um equipamento pelo ID.
     *
     * @throws jakarta.persistence.EntityNotFoundException se não encontrado
     */
    public EquipmentGuide getById(UUID id) {
        return repository.findById(id)
                .orElseThrow(() -> new jakarta.persistence.EntityNotFoundException(
                        "Equipamento não encontrado: " + id));
    }

    /**
     * Cria um novo equipamento com imagem opcional.
     *
     * @param dto   DTO do objeto
     * @param imagem Arquivo de imagem (pode ser null)
     * @return Entidade persistida
     */
    @Transactional
    public EquipmentGuide create(ProductEquipmentCreateDTO dto, MultipartFile imagem) throws IOException {
        EquipmentGuide entity = new EquipmentGuide();
        entity.setNome(dto.nome());

        if (imagem != null && !imagem.isEmpty()) {
            String filename = storage.saveImage(imagem, dto.nome());
            entity.setImagem(filename);
        }

        return repository.save(entity);
    }

    /**
     * Atualiza nome e/ou imagem de um equipamento existente.
     *
     * @param id     ID do equipamento
     * @param dto    DTO com novos atributos (null = mantém o atual)
     * @param imagem Novo arquivo de imagem (null = mantém a atual)
     * @return Entidade atualizada
     */
    @Transactional
    public EquipmentGuide update(UUID id, ProductEquipmentUpdateDTO dto, MultipartFile imagem) throws IOException {
        EquipmentGuide entity = getById(id);

        if (dto.nome() != null && !dto.nome().isBlank()) {
            entity.setNome(dto.nome());
        }

        if (imagem != null && !imagem.isEmpty()) {
            String filename = storage.saveImage(imagem, entity.getNome());
            entity.setImagem(filename);
        }

        return repository.save(entity);
    }

    /**
     * Remove um equipamento pelo ID.
     *
     * @param id ID do equipamento
     */
    @Transactional
    public void delete(UUID id) {
        if (repository.existsById(id)) repository.deleteById(id);
    }
}