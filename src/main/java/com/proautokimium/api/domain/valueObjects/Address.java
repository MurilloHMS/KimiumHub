package com.proautokimium.api.domain.valueObjects;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.StringJoiner;

/**
 * Um endereço brasileiro, em partes.
 *
 * <p>Embutido em {@code companies}, {@code company_events} e {@code event_talks},
 * com as mesmas colunas {@code address_*} nas três (V106). Em partes e não numa
 * linha só: o mapa e os apps de rota procuram pelo texto montado em
 * {@link #formatted()}, mas recibo, holerite e nota vão querer cada parte.
 *
 * <p><b>Todo campo é opcional aqui.</b> Quem exige o mínimo para um endereço
 * servir é quem usa — a palestra fora da empresa exige rua e cidade; a empresa
 * do RH pode nem ter endereço.
 */
@Embeddable
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode
public class Address {

    @Column(name = "address_zip_code", length = 9)
    private String zipCode;

    @Column(name = "address_street", length = 150)
    private String street;

    @Column(name = "address_number", length = 20)
    private String number;

    @Column(name = "address_complement", length = 100)
    private String complement;

    @Column(name = "address_district", length = 100)
    private String district;

    @Column(name = "address_city", length = 100)
    private String city;

    @Column(name = "address_state", length = 2)
    private String state;

    /** Sem rua e sem cidade não há o que mandar para o mapa. */
    public boolean isUsable() {
        return hasText(street) && hasText(city);
    }

    public boolean isEmpty() {
        return !hasText(zipCode) && !hasText(street) && !hasText(number) && !hasText(complement)
                && !hasText(district) && !hasText(city) && !hasText(state);
    }

    /**
     * "Av. Colombo, 5790 - Zona 7, Maringá - PR, 87020-900".
     *
     * <p>É o texto que vai para o Google Maps, o Waze, o Apple Maps e o Uber. O
     * complemento fica de fora de propósito: "Bloco A" confunde a busca e não
     * muda o ponto no mapa.
     */
    public String formatted() {
        StringJoiner linha = new StringJoiner(" - ");

        String rua = hasText(number) ? join(", ", street, number) : trim(street);
        if (hasText(rua)) linha.add(rua);
        if (hasText(district)) linha.add(district.trim());

        String cidade = hasText(state) ? join(" - ", city, state.trim().toUpperCase()) : trim(city);
        String resultado = linha.toString();
        if (hasText(cidade)) resultado = hasText(resultado) ? resultado + ", " + cidade : cidade;
        if (hasText(zipCode)) resultado = hasText(resultado) ? resultado + ", " + zipCode.trim() : zipCode.trim();

        return resultado;
    }

    private static String join(String separador, String a, String b) {
        if (!hasText(a)) return trim(b);
        if (!hasText(b)) return trim(a);
        return a.trim() + separador + b.trim();
    }

    private static String trim(String valor) {
        return valor == null ? "" : valor.trim();
    }

    private static boolean hasText(String valor) {
        return valor != null && !valor.isBlank();
    }
}
