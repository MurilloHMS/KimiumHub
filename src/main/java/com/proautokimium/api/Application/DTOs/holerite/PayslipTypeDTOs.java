package com.proautokimium.api.Application.DTOs.holerite;

import com.proautokimium.api.domain.entities.PayslipType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** Os DTOs dos tipos de holerite: pequenos e só fazem sentido juntos. */
public final class PayslipTypeDTOs {

    private PayslipTypeDTOs() {
    }

    public record PayslipTypeDTO(String code, String label) {
        public static PayslipTypeDTO from(PayslipType t) {
            return new PayslipTypeDTO(t.getCode(), t.getLabel());
        }
    }

    public record CreatePayslipTypeRequest(
            @NotBlank(message = "Informe o nome do tipo.")
            @Size(max = PayslipType.LABEL_MAX, message = "O nome tem no máximo 60 caracteres.")
            String label) {
    }

    /**
     * @param created falso quando já existia um tipo com o mesmo nome (sem
     *                contar caixa nem acento): a tela seleciona o existente em
     *                vez de criar "PLR" e "plr" lado a lado
     */
    public record CreatePayslipTypeResult(PayslipTypeDTO type, boolean created) {
    }
}
