package com.proautokimium.api.controllers.processoSeletivo;

import com.proautokimium.api.Infrastructure.services.processoSeletivo.StorageService;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

@RestController
@RequestMapping("api/curriculos")
public class CurriculoController {

    private final StorageService storageService;

    public CurriculoController(StorageService storageService){
        this.storageService = storageService;
    }

    @GetMapping("/{fileName}")
    public ResponseEntity<Resource> getCurriculo(@PathVariable String fileName) throws IOException {
        Path path = storageService.buscarCurriculo(fileName);

        if(!Files.exists(path))
            return ResponseEntity.notFound().build();

        Resource resource = new UrlResource(path.toUri());
        String contentType = Files.probeContentType(path);

        if (contentType == null || contentType.isBlank()) {
            contentType = "application/pdf";
        }

        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(contentType))
                .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + fileName + "\"")
                .body(resource);
    }
}
