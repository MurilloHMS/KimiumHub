package com.proautokimium.api.Infrastructure.interfaces.pdf;

import com.proautokimium.api.Application.DTOs.pdf.PdfPageInfo;

import java.util.List;

public interface IPdfReader {
    List<PdfPageInfo> getPdfByPage(String inputPdfPath);
}
