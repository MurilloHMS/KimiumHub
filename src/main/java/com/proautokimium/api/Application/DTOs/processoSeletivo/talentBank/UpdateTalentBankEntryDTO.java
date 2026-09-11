package com.proautokimium.api.Application.DTOs.processoSeletivo.talentBank;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * O que a pessoa pode corrigir pelo link do e-mail.
 *
 * <p><b>Não tem {@code email}, e isso é de propósito.</b> Trocar o e-mail por
 * token permitiria mover o cadastro para um endereço que o portador do link
 * controla — e colidiria com o índice único. Quem mudou de endereço faz uma
 * inscrição nova e apaga a antiga.
 *
 * @param consentimento marcar de novo <b>renova</b> o prazo a partir de agora.
 *                      É todo o mecanismo de renovação, e não custa nenhuma
 *                      infraestrutura a mais
 */
public record UpdateTalentBankEntryDTO(

        @NotBlank(message = "Informe seu nome.")
        @Size(max = 100, message = "O nome deve ter no máximo 100 caracteres.")
        String nome,

        @NotBlank(message = "Informe seu telefone.")
        @Size(max = 11, message = "O telefone deve ter no máximo 11 dígitos.")
        String telefone,

        @Size(max = 100, message = "O LinkedIn deve ter no máximo 100 caracteres.")
        String urlLinkedin,

        @Size(max = 100, message = "A área deve ter no máximo 100 caracteres.")
        String areaInteresse,

        boolean consentimento
) { }
