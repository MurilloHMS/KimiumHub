package com.proautokimium.api.Application.DTOs.pdf;

public record PdfPageInfoExtractorDTO(
        String nome,
        String cargo,
        String cpf,
        String empresa,
        String departamento,
        Double inss,
        Double irrf,
        Double inssFerias,
        Double irrfFerias,
        Double inss13,
        Double irrf13,
        Double fgts,
        Double emprestimoTrabalhador
) {
}
