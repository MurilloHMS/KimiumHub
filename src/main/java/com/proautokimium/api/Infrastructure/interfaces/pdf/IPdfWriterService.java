package com.proautokimium.api.Infrastructure.interfaces.pdf;

import com.proautokimium.api.Application.DTOs.pdf.PdfPageInfoDTO;

import java.util.List;

public interface IPdfWriterService {
    void SavePages(String inputPdfPath, String outputFolder, List<PdfPageInfoDTO> pages);
}
