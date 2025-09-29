package com.proautokimium.api.Infrastructure.interfaces.pdf;

import com.proautokimium.api.Application.DTOs.pdf.PdfPageInfoDTO;

import java.util.List;

public interface IPdfReader {
    List<PdfPageInfoDTO> getPdfByPage(String inputPdfPath);
}
