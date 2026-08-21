package com.proautokimium.api.Infrastructure.services.product;

import com.proautokimium.api.Application.DTOs.product.ProductWebSiteCreateDTO;
import com.proautokimium.api.Application.DTOs.product.ProductWebSitePublicResponseDTO;
import com.proautokimium.api.Application.DTOs.product.ProductWebSiteResponseDTO;
import com.proautokimium.api.Application.DTOs.product.ProductWebSiteUpdateDTO;
import com.proautokimium.api.Infrastructure.converters.ProductWebSiteConverter;
import com.proautokimium.api.Infrastructure.exceptions.product.ProductNotFoundException;
import com.proautokimium.api.Infrastructure.repositories.EquipmentGuideRepository;
import com.proautokimium.api.Infrastructure.repositories.ProductWebSiteRepository;
import com.proautokimium.api.Infrastructure.services.gallery.GalleryDocumentService;
import com.proautokimium.api.Infrastructure.services.storage.ProductImageStorageService;
import com.proautokimium.api.domain.entities.EquipmentGuide;
import com.proautokimium.api.domain.entities.ProductWebsite;
import com.proautokimium.api.domain.entities.gallery.GalleryDocument;
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
    private final GalleryDocumentService galleryService;

    public ProductWebsiteService(ProductWebSiteRepository repository, ProductWebSiteConverter converter,
                                 ProductImageStorageService storage, EquipmentGuideRepository equipmentRepository,
                                 GalleryDocumentService galleryService) {
        this.repository = repository;
        this.converter = converter;
        this.storage = storage;
        this.equipmentRepository = equipmentRepository;
        this.galleryService = galleryService;
    }

    @Transactional
    public void create(ProductWebSiteCreateDTO dto, MultipartFile imagem) throws IOException {
        ProductWebsite entity = converter.fromCreateDto(dto);
        applyEquipment(entity, dto.equipmentId());
        applyImage(entity, imagem, dto.galleryDocumentId(), dto.systemCode());

        repository.save(entity);
    }

    @Transactional
    public void update(ProductWebSiteUpdateDTO dto, UUID id, MultipartFile imagem) throws IOException {
        ProductWebsite entity = repository.findById(id).orElseThrow(ProductNotFoundException::new);
        converter.updateFromDto(dto, entity);
        applyEquipment(entity, dto.equipmentId());
        applyImage(entity, imagem, dto.galleryDocumentId(), entity.getSystemCode());

        repository.save(entity);
    }

    /**
     * Define a imagem do produto, venha ela de upload ou da galeria.
     *
     * Escolher da galeria **copia os bytes** para o acervo do produto, em vez
     * de apontar para o arquivo de lá. A galeria é a fonte, não o servidor, e a
     * cópia é o que mantém três coisas de pé:
     *
     * - A vitrine pública. `/upload/images/**` está no PUBLIC_GET; o acervo da
     *   galeria não está, e abrir ele exporia catálogo e arte interna a quem
     *   souber o nome do arquivo.
     * - O guia. O GuideReportService monta o PDF lendo a imagem do **disco**,
     *   pelo ProductImageStorageService, não por URL. Apontar para a galeria
     *   quebraria o guia em silêncio: a célula sai vazia, sem erro e sem log.
     * - O acervo. Apagar a foto da galeria deixa de poder quebrar o site.
     *
     * Nenhum dos dois informado mantém a imagem que já estava — é o que faz o
     * update sem troca de foto continuar funcionando.
     */
    private void applyImage(ProductWebsite entity, MultipartFile imagem,
                            UUID galleryDocumentId, String prefix) throws IOException {
        // Arquivo enviado ganha da galeria: se vieram os dois, o upload é o
        // gesto mais deliberado.
        if (imagem != null && !imagem.isEmpty()) {
            entity.setImagem(storage.save(imagem, prefix));
            return;
        }

        if (galleryDocumentId == null) return;

        GalleryDocument documento = galleryService.findById(galleryDocumentId);
        byte[] bytes = galleryService.getFile(galleryDocumentId);
        entity.setImagem(storage.save(bytes, documento.getOriginalFileName(), prefix));
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