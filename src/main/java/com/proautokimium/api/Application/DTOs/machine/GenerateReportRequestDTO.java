package com.proautokimium.api.Application.DTOs.machine;

import lombok.Getter;
import lombok.Setter;

import java.util.Map;

@Getter
@Setter
public class GenerateReportRequestDTO {
    private String processId;
    private String mesReferencia;
    private Map<String, String> vencimentos;
}
