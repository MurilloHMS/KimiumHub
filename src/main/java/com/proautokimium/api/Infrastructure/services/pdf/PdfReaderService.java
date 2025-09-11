package com.proautokimium.api.Infrastructure.services.pdf;

import com.proautokimium.api.Application.DTOs.pdf.PdfPageInfo;
import com.proautokimium.api.Infrastructure.interfaces.pdf.INameExtractor;
import com.proautokimium.api.Infrastructure.interfaces.pdf.IPdfReader;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

@Service
public class PdfReaderService implements IPdfReader {

    private final INameExtractor nameExtractor;

    public PdfReaderService(INameExtractor nameExtractor){
        this.nameExtractor = nameExtractor;
    }

    @Override
    public List<PdfPageInfo> getPdfByPage(String inputPdfPath){
        File file = new File(inputPdfPath);
        if(!file.exists()){
            throw new IllegalArgumentException("PDF file not found: " + inputPdfPath);
        }

        List<PdfPageInfo> files = new ArrayList<>();

        try (PDDocument document = Loader.loadPDF(file)) {
            PDFTextStripper stripper = new PDFTextStripper();
            int totalPages = document.getNumberOfPages();

            for (int i = 1; i <= totalPages; i++) {
                stripper.setStartPage(i);
                stripper.setEndPage(i);

                String textPage = stripper.getText(document);
                String employeeName = nameExtractor.extractName(textPage);

                String pageName = (employeeName != null && !employeeName.isEmpty())
                        ? employeeName
                        : "Page_" + i;

                PdfPageInfo pageInfo = new PdfPageInfo(pageName);
                files.add(pageInfo);
            }

            return files;

        } catch (IOException e) {
            throw new RuntimeException("Error reading file " + inputPdfPath, e);
        }
    }
}
