package com.proautokimium.api.Application.DTOs.address;

import com.proautokimium.api.domain.valueObjects.Address;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * O endereço nas duas direções: o que o site manda e o que ele recebe.
 *
 * @param formatted só na resposta — é o texto que o site entrega ao mapa e aos
 *                  apps de rota. Na requisição é ignorado.
 */
public record AddressDTO(
        @Pattern(regexp = "^$|^\\d{5}-?\\d{3}$", message = "CEP inválido.")
        String zipCode,

        @Size(max = 150, message = "O logradouro deve ter no máximo 150 caracteres.")
        String street,

        @Size(max = 20, message = "O número deve ter no máximo 20 caracteres.")
        String number,

        @Size(max = 100, message = "O complemento deve ter no máximo 100 caracteres.")
        String complement,

        @Size(max = 100, message = "O bairro deve ter no máximo 100 caracteres.")
        String district,

        @Size(max = 100, message = "A cidade deve ter no máximo 100 caracteres.")
        String city,

        @Pattern(regexp = "^$|^[A-Za-z]{2}$", message = "UF deve ter duas letras.")
        String state,

        String formatted
) {

    public static AddressDTO from(Address address) {
        if (address == null || address.isEmpty()) {
            return null;
        }
        return new AddressDTO(address.getZipCode(), address.getStreet(), address.getNumber(),
                address.getComplement(), address.getDistrict(), address.getCity(), address.getState(),
                address.formatted());
    }

    /**
     * Para a entidade, com os campos em branco virando {@code null}.
     *
     * <p>Devolve {@code null} para endereço vazio: o Hibernate grava as sete
     * colunas nulas, e a leitura volta como "sem endereço" em vez de um objeto
     * cheio de strings vazias que parece preenchido.
     */
    public Address toAddress() {
        Address address = new Address(
                clean(zipCode), clean(street), clean(number), clean(complement),
                clean(district), clean(city), state == null || state.isBlank() ? null : state.trim().toUpperCase());
        return address.isEmpty() ? null : address;
    }

    private static String clean(String valor) {
        return valor == null || valor.isBlank() ? null : valor.trim();
    }
}
