package com.proautokimium.api.Infrastructure.interfaces.pdf;

import com.proautokimium.api.Application.DTOs.pdf.PdfPageInfo;

import java.util.List;

public interface IPdfProcessingService {
    List<PdfPageInfo> GetPdfByPage(String inputPdfPath);
    void SavePages(String inputPdfPath, String outputFolder, List<PdfPageInfo> pages);
}
