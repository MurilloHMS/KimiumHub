package com.proautokimium.api.controllers.tools;

import com.proautokimium.api.Infrastructure.services.tools.PDFUnlocker;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

@RestController
@RequestMapping("api/tools/pdf")
public class PDFToolsController {

    @PreAuthorize("hasAuthority('tools/pdf/unlock:INCLUIR')")
    @PostMapping("/unlock")
    public ResponseEntity<byte[]> unlockPdfFile(@RequestParam("file") MultipartFile file, @RequestParam("password") String password) throws IOException {
        byte[] unlockedFile = PDFUnlocker.unlock(file.getBytes(), password);
        return ResponseEntity.ok()
                .header(
                        HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=\"unlocked_file.pdf\""
                ).contentType(MediaType.APPLICATION_PDF)
                .body(unlockedFile);
    }
}
