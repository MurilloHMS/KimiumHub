package com.proautokimium.api.Application.DTOs.sales;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.util.UUID;

/** Os dois cadastros do checklist que a Controladoria mantém. */
public final class ChecklistRegisterDTO {

    private ChecklistRegisterDTO() {
    }

    public record VisualItem(UUID id, String name, int sortOrder, boolean active) {}

    public record VisualItemRequest(@NotBlank @Size(max = 150) String name, int sortOrder, boolean active) {}

    /** {@code erpName} vem do Sankhya; nulo quando o produto não está mais ativo lá. */
    public record ComodatoItem(UUID id, int productCode, String erpName, String popularName,
                               int sortOrder, boolean active) {}

    public record ComodatoItemRequest(int productCode, @Size(max = 100) String popularName,
                                      int sortOrder, boolean active) {}

    /** Um produto dos grupos de comodato do Sankhya, para escolher. */
    public record ComodatoCandidate(int productCode, String name, long group, boolean chosen) {}
}
