package com.proautokimium.api.Infrastructure.services.signature;

import com.proautokimium.api.Application.DTOs.signature.BackgroundResponseDTO;
import com.proautokimium.api.Application.DTOs.signature.TemplateResponseDTO;
import com.proautokimium.api.Infrastructure.exceptions.emailSignature.TemplateOfEmailSignatureNotExistException;
import com.proautokimium.api.domain.exceptions.file.FileNotImageException;
import com.proautokimium.api.Infrastructure.repositories.email.signature.EmailSignatureTemplateRepository;
import com.proautokimium.api.Infrastructure.services.storage.SignatureBackgroundStorageService;
import com.proautokimium.api.domain.entities.EmailSignatureTemplate;
import jakarta.transaction.Transactional;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.time.Clock;
import java.time.LocalDateTime;

@Service
public class EmailSignatureTemplateService {

    private final EmailSignatureTemplateRepository repository;
    private final SignatureBackgroundStorageService storage;
    private final Clock clock;

    public EmailSignatureTemplateService(EmailSignatureTemplateRepository repository,
                                         SignatureBackgroundStorageService storage, Clock clock) {
        this.repository = repository;
        this.storage = storage;
        this.clock = clock;
    }

    @Transactional
    public TemplateResponseDTO find(){
        EmailSignatureTemplate template = load();
        return new TemplateResponseDTO(
                template.getDocument(),
                template.getUpdatedAt(),
                template.getUpdatedBy()
        );
    }

    @Transactional
    public TemplateResponseDTO save(String document, String login){
        EmailSignatureTemplate template = load();
        template.alterar(document, login, LocalDateTime.now(clock));
        repository.save(template);
        return new TemplateResponseDTO(
                template.getDocument(),
                template.getUpdatedAt(),
                template.getUpdatedBy()
        );
    }

    public BackgroundResponseDTO sendBackground(MultipartFile file) throws IOException {
        // Ler as dimensões ANTES de gravar: um arquivo que o Java não consegue
        // decodificar não é imagem, e é melhor recusar aqui do que descobrir no
        // navegador do designer.
        BufferedImage image = ImageIO.read(file.getInputStream());
        if(image == null) throw new FileNotImageException();

        String path = storage.save(file, "background");
        return new BackgroundResponseDTO(
                path, image.getWidth(), image.getHeight()
        );
    }

    /**
     * A migration semeia a linha, então ela sempre existe. Se não existir, algo
     * saiu muito errado — e falhar alto aqui é melhor que devolver um template
     * vazio, que o front desenharia como uma imagem em branco.
     */
    private EmailSignatureTemplate load(){
        return repository.findFirstByOrderByUpdatedAtDesc()
                .orElseThrow(TemplateOfEmailSignatureNotExistException::new);
    }
}
