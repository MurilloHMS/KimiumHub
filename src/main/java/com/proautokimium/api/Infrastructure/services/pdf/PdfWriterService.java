package com.proautokimium.api.Infrastructure.services.pdf;

import com.proautokimium.api.Application.DTOs.pdf.PdfPageInfo;
import com.proautokimium.api.Infrastructure.interfaces.pdf.IFileNameSanitizerService;
import com.proautokimium.api.Infrastructure.interfaces.pdf.IPdfWriterService;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Locale;

@Service
public class PdfWriterService implements IPdfWriterService {

    private final IFileNameSanitizerService fileNameSanitizerService;

    public PdfWriterService(IFileNameSanitizerService fileNameSanitizerService){
        this.fileNameSanitizerService = fileNameSanitizerService;
    }

    @Override
    public void SavePages(String inputPdfPath, String outputFolder, List<PdfPageInfo> pages) {
        File inputFile = new File(inputPdfPath);

        if (!inputFile.exists() || pages == null || pages.isEmpty()) {
            return;
        }

        File outputDir = new File(outputFolder);
        if(!outputDir.exists()){
            outputDir.mkdirs();
        }

        try(PDDocument inputDocument = Loader.loadPDF(inputFile)){
            if(pages.size() != inputDocument.getNumberOfPages()){
                throw new IllegalArgumentException("The number of pages does not match the input document");
            }

            for(int pageNumber = 0; pageNumber < inputDocument.getNumberOfPages(); pageNumber++){
                try(PDDocument outputDocument = new PDDocument()){
                    outputDocument.addPage(inputDocument.getPage(pageNumber));

                    String filename = fileNameSanitizerService.Sanitize(pages.get(pageNumber).name());
                    File outputFile = new File(outputDir, filename + ".pdf");

                    outputDocument.save(outputFile);
                }
            }
        }catch (IOException e){
            throw new RuntimeException("Erro ao salvar PDFs em: " + outputFolder, e);
        }
    }
}
