package com.proautokimium.api.Application.DTOs.processoSeletivo.talentBank;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Inscrição espontânea: currículo sem vaga aberta.
 *
 * <p>Os DTOs deste módulo não tinham anotação de validação nenhuma. Numa rota
 * pública isso significa que o {@code @Valid} do controller não valida nada, e
 * campo em branco desce até o banco para morrer em constraint — 500 em vez de
 * 400.
 *
 * @param consentimento obrigatório <b>aqui</b>, ao contrário do formulário de
 *                      vaga: candidatar-se a uma vaga específica é finalidade
 *                      legítima por si só; ficar no banco para vagas futuras é
 *                      outra coisa, e precisa de aceite separado
 */
public record CreateTalentBankEntryDTO(

        @NotBlank(message = "Informe seu nome.")
        @Size(max = 100, message = "O nome deve ter no máximo 100 caracteres.")
        String nome,

        @NotBlank(message = "Informe seu e-mail.")
        @Email(message = "E-mail inválido.")
        @Size(max = 100, message = "O e-mail deve ter no máximo 100 caracteres.")
        String email,

        @NotBlank(message = "Informe seu telefone.")
        @Size(max = 11, message = "O telefone deve ter no máximo 11 dígitos.")
        String telefone,

        @Size(max = 100, message = "O LinkedIn deve ter no máximo 100 caracteres.")
        String urlLinkedin,

        @Size(max = 100, message = "A área deve ter no máximo 100 caracteres.")
        String areaInteresse,

        @AssertTrue(message = "É preciso autorizar a guarda dos seus dados.")
        boolean consentimento
) { }
