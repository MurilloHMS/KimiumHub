package com.proautokimium.api.Infrastructure.services.reports.guide;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.proautokimium.api.Application.DTOs.guide.GuideLayoutCatalogDTO;
import com.proautokimium.api.Application.DTOs.guide.GuideLayoutDTO;
import com.proautokimium.api.Application.DTOs.guide.GuideLayoutImageDTO;
import com.proautokimium.api.Application.DTOs.guide.GuideLayoutStateDTO;
import com.proautokimium.api.Application.DTOs.guide.GuideLayoutVersionDTO;
import com.proautokimium.api.Infrastructure.exceptions.guide.GuideLayoutImageNotFoundException;
import com.proautokimium.api.Infrastructure.exceptions.guide.GuideLayoutNotFoundException;
import com.proautokimium.api.Infrastructure.exceptions.guide.InvalidGuideLayoutException;
import com.proautokimium.api.Infrastructure.repositories.guide.GuideLayoutImageRepository;
import com.proautokimium.api.Infrastructure.repositories.guide.GuideLayoutRepository;
import com.proautokimium.api.domain.entities.guide.GuideLayout;
import com.proautokimium.api.domain.entities.guide.GuideLayoutImage;
import com.proautokimium.api.domain.enums.guide.GuideField;
import com.proautokimium.api.domain.enums.guide.GuideImageSource;
import com.proautokimium.api.domain.enums.guide.GuideLayoutStatus;
import com.proautokimium.api.domain.exceptions.file.FileNotImageException;
import com.proautokimium.api.domain.valueObjects.guide.GuideLayoutDocument;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * O ciclo do layout do guia: rascunho, publicação e volta a uma versão antiga.
 *
 * A regra que sustenta o resto: **Contratos só vê o que foi publicado**. O
 * designer salva quantos rascunhos quiser, e nada muda no guia de ninguém até
 * ele clicar em Publicar. Restaurar uma versão antiga também não publica — ela
 * vira o rascunho, para ser conferida na prévia antes.
 */
@Service
public class GuideLayoutService {

    private static final long MAX_IMAGE_BYTES = 2L * 1024 * 1024;
    private static final Set<String> IMAGE_TYPES = Set.of("image/png", "image/jpeg");

    private final GuideLayoutRepository repository;
    private final GuideLayoutImageRepository images;
    private final GuideLayoutValidator validator;
    private final ObjectMapper objectMapper;
    private final Clock clock;

    public GuideLayoutService(GuideLayoutRepository repository, GuideLayoutImageRepository images,
                              GuideLayoutValidator validator, ObjectMapper objectMapper, Clock clock) {
        this.repository = repository;
        this.images = images;
        this.validator = validator;
        this.objectMapper = objectMapper;
        this.clock = clock;
    }

    @Transactional(readOnly = true)
    public GuideLayoutStateDTO state() {
        return new GuideLayoutStateDTO(
                GuideLayoutDTO.from(published()),
                GuideLayoutDTO.from(repository.findFirstByStatus(GuideLayoutStatus.DRAFT).orElse(null)));
    }

    public GuideLayoutCatalogDTO catalog() {
        return new GuideLayoutCatalogDTO(
                Arrays.stream(GuideField.values())
                        .map(f -> new GuideLayoutCatalogDTO.Field(f.name(), f.getLabel(), f.getKind().name()))
                        .toList(),
                Arrays.stream(GuideImageSource.values())
                        .map(s -> new GuideLayoutCatalogDTO.Option(s.name(), s.getLabel()))
                        .toList(),
                GuideLayoutValidator.FONTS);
    }

    /** Cria o rascunho se não houver, ou o sobrescreve. Layout inválido não é salvo. */
    @Transactional
    public GuideLayoutDTO saveDraft(JsonNode document, String login) {
        String text = parseAndValidate(document).text();
        LocalDateTime now = LocalDateTime.now(clock);
        GuideLayout draft = repository.findFirstByStatus(GuideLayoutStatus.DRAFT)
                .map(existing -> {
                    existing.updateDraft(text, login, now);
                    return existing;
                })
                .orElseGet(() -> GuideLayout.draft(text, login, now));
        return GuideLayoutDTO.from(repository.save(draft));
    }

    @Transactional
    public void discardDraft() {
        repository.findFirstByStatus(GuideLayoutStatus.DRAFT).ifPresent(repository::delete);
    }

    /**
     * O rascunho vira a versão em uso, e a anterior vai para o arquivo.
     *
     * Arquivar vem antes, e com flush: o índice parcial só aceita um
     * PUBLISHED, e o Hibernate não garante a ordem dos UPDATEs no commit.
     */
    @Transactional
    public GuideLayoutDTO publish(String note, String login) {
        GuideLayout draft = repository.findFirstByStatus(GuideLayoutStatus.DRAFT)
                .orElseThrow(() -> new GuideLayoutNotFoundException("Não há rascunho para publicar"));
        // O rascunho foi validado ao salvar, mas o validador pode ter ficado
        // mais rígido desde então. Publicar algo que não gera PDF derrubaria
        // o guia de Contratos.
        parseAndValidate(draft.getDocument());

        GuideLayout current = repository.findFirstByStatus(GuideLayoutStatus.PUBLISHED).orElse(null);
        if (current != null) {
            current.archive();
            repository.saveAndFlush(current);
        }
        String trimmed = note == null || note.isBlank() ? null : note.trim();
        draft.publish(repository.findMaxVersion() + 1, trimmed, login, LocalDateTime.now(clock));
        return GuideLayoutDTO.from(repository.save(draft));
    }

    @Transactional(readOnly = true)
    public List<GuideLayoutVersionDTO> versions() {
        return repository.findByStatusInOrderByVersionDesc(List.of(GuideLayoutStatus.PUBLISHED, GuideLayoutStatus.ARCHIVED))
                .stream().map(GuideLayoutVersionDTO::from).toList();
    }

    /** A versão antiga vira o rascunho. Não publica: o designer confere antes. */
    @Transactional
    public GuideLayoutDTO restore(UUID versionId, String login) {
        GuideLayout source = repository.findById(versionId)
                .filter(l -> l.getStatus() != GuideLayoutStatus.DRAFT)
                .orElseThrow(() -> new GuideLayoutNotFoundException("Versão não encontrada"));
        LocalDateTime now = LocalDateTime.now(clock);
        GuideLayout draft = repository.findFirstByStatus(GuideLayoutStatus.DRAFT)
                .map(existing -> {
                    existing.updateDraft(source.getDocument(), login, now);
                    return existing;
                })
                .orElseGet(() -> GuideLayout.draft(source.getDocument(), login, now));
        return GuideLayoutDTO.from(repository.save(draft));
    }

    /** O publicado, já lido. A migration semeia um, então ele sempre existe. */
    @Transactional(readOnly = true)
    public ParsedLayout publishedLayout() {
        return parseAndValidate(published().getDocument());
    }

    public ParsedLayout parseAndValidate(JsonNode document) {
        try {
            return parseAndValidate(objectMapper.writeValueAsString(document));
        } catch (JsonProcessingException e) {
            throw new InvalidGuideLayoutException("O layout não pôde ser lido");
        }
    }

    public ParsedLayout parseAndValidate(String text) {
        GuideLayoutDocument layout;
        try {
            layout = objectMapper.readValue(text, GuideLayoutDocument.class);
        } catch (JsonProcessingException e) {
            throw new InvalidGuideLayoutException("O layout não pôde ser lido: " + e.getOriginalMessage());
        }
        validator.validate(layout);
        return new ParsedLayout(text, layout);
    }

    // ── Imagens enviadas pelo designer ──────────────────────────────────────

    @Transactional
    public GuideLayoutImageDTO saveImage(MultipartFile file, String login) throws IOException {
        if (file.isEmpty()) throw new FileNotImageException();
        if (file.getSize() > MAX_IMAGE_BYTES) {
            throw new InvalidGuideLayoutException("A imagem tem mais de 2 MB. Reduza antes de enviar");
        }
        byte[] bytes = file.getBytes();
        // Decodificar é o que prova que é imagem: a extensão e o content type
        // são o que o navegador diz, e o Jasper recusaria no meio do PDF.
        BufferedImage decoded = ImageIO.read(new ByteArrayInputStream(bytes));
        String type = file.getContentType();
        if (decoded == null || type == null || !IMAGE_TYPES.contains(type)) throw new FileNotImageException();

        GuideLayoutImage image = images.save(new GuideLayoutImage(bytes, type, file.getOriginalFilename(),
                decoded.getWidth(), decoded.getHeight(), login, LocalDateTime.now(clock)));
        return new GuideLayoutImageDTO(image.getId(), image.getWidth(), image.getHeight());
    }

    @Transactional(readOnly = true)
    public GuideLayoutImage image(UUID id) {
        return images.findById(id).orElseThrow(GuideLayoutImageNotFoundException::new);
    }

    private GuideLayout published() {
        return repository.findFirstByStatus(GuideLayoutStatus.PUBLISHED)
                .orElseThrow(() -> new IllegalStateException(
                        "Não há layout do guia publicado. A V115 semeia um; alguém apagou a linha?"));
    }

    /** O texto e o documento lido andam juntos: o texto é a chave do cache de compilação. */
    public record ParsedLayout(String text, GuideLayoutDocument layout) {}
}
