package com.proautokimium.api.controllers;

import com.proautokimium.api.Application.DTOs.pdf.PdfPageInfo;
import com.proautokimium.api.Infrastructure.services.pdf.PdfReaderService;
import com.proautokimium.api.Infrastructure.services.pdf.PdfWriterService;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

@RestController
@RequestMapping("api/pdf")
public class PdfController {
    private final PdfReaderService pdfReaderService;
    private final PdfWriterService pdfWriterService;
    private static final Map<String, File> uploadedFiles = new ConcurrentHashMap<>();

    public PdfController(PdfReaderService pdfReaderService,
                              PdfWriterService pdfWriterService) {
        this.pdfReaderService = pdfReaderService;
        this.pdfWriterService = pdfWriterService;
    }

    @PostMapping("/upload")
    public ResponseEntity<?> upload(@RequestParam("file")MultipartFile file){
        if(file == null || file.isEmpty()){
            return ResponseEntity.badRequest().body("Nenhum arquivo ou arquivo inválido enviado");
        }

        try{
            File tempFile = File.createTempFile("upload_", ".pdf");
            file.transferTo(tempFile);

            List<PdfPageInfo> result = pdfReaderService.getPdfByPage(tempFile.getAbsolutePath());

            if(result.isEmpty()){
                return ResponseEntity.badRequest().body("Não foi possível extrair as páginas do PDF");
            }

            String uploadId = UUID.randomUUID().toString();
            uploadedFiles.put(uploadId, tempFile);

            return ResponseEntity.ok(Map.of(
                    "uploadId", uploadId,
                    "pages", result
            ));
        }catch (IOException e){
            return ResponseEntity.internalServerError().body("Erro ao processar o arquivo: " + e.getMessage());
        }
    }

    @PostMapping("/save")
    public ResponseEntity<?> save(@PathVariable String uploadId, @RequestBody List<PdfPageInfo> pages){
        if (pages == null || pages.isEmpty()) {
            return ResponseEntity.badRequest().body("Nenhuma página fornecida para salvar.");
        }

        File inputPdfPath = uploadedFiles.get(uploadId);
        if(inputPdfPath == null){
            return ResponseEntity.badRequest().body("Upload não encontrado ou expirado");
        }

        try{
            File outputDir = Files.createTempDirectory("split_pdfs").toFile();

            pdfWriterService.SavePages(inputPdfPath.getAbsolutePath(), outputDir.getAbsolutePath(), pages);

            File zipFile = File.createTempFile("pdfs_", ".zip");
            try(FileOutputStream fos = new FileOutputStream(zipFile);
                ZipOutputStream zipOut = new ZipOutputStream(fos)){

                for(File pdf : outputDir.listFiles()){
                    try(FileInputStream fis = new FileInputStream(pdf)){
                        ZipEntry zipEntry = new ZipEntry(pdf.getName());
                        zipOut.putNextEntry(zipEntry);

                        byte[] bytes = fis.readAllBytes();
                        zipOut.write(bytes, 0, bytes.length);
                        zipOut.closeEntry();
                    }
                }
            }

            byte[] zipBytes = Files.readAllBytes(zipFile.toPath());

            return ResponseEntity.ok().header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=PDF_SEPARADOS.zip")
                    .contentType(MediaType.APPLICATION_OCTET_STREAM)
                    .body(zipBytes);
        }
        catch(Exception e){
            return ResponseEntity.internalServerError().body("Erro ao salvar PDFs: " + e.getMessage());
        }
    }

}
