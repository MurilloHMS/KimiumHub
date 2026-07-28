package com.proautokimium.api.Application.DTOs.machine;

import lombok.Getter;
import lombok.Setter;

import java.util.List;
import java.util.Map;

@Getter
@Setter
public class GenerateReportRequestV2DTO {
    private String processId;
    private String mesReferencia;
    private int anoReferencia;
    private String mode;
    private Map<String, String> vencimentos;
    private Map<String, String> nomeOverrides;
    private List<String> excludedKeys;
}
