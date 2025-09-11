package com.proautokimium.api.Infrastructure.services.pdf;

import com.proautokimium.api.Application.DTOs.pdf.PdfPageInfo;
import com.proautokimium.api.Infrastructure.interfaces.pdf.IPdfProcessingService;
import com.proautokimium.api.Infrastructure.interfaces.pdf.IPdfReader;
import com.proautokimium.api.Infrastructure.interfaces.pdf.IPdfWriterService;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class PdfProcessingService implements IPdfProcessingService {

    private final IPdfReader pdfReader;
    private final IPdfWriterService pdfWriterService;

    public PdfProcessingService(IPdfReader reader, IPdfWriterService writer){
        this.pdfReader = reader;
        this.pdfWriterService = writer;
    }
    @Override
    public List<PdfPageInfo> GetPdfByPage(String inputPdfPath) {
        return pdfReader.getPdfByPage(inputPdfPath);
    }

    @Override
    public void SavePages(String inputPdfPath, String outputFolder, List<PdfPageInfo> pages) {
        pdfWriterService.SavePages(inputPdfPath, outputFolder, pages);
    }
}
