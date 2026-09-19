package com.proautokimium.api.domain.valueObjects;

import com.proautokimium.api.Application.DTOs.address.AddressDTO;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

class AddressTest {

    @Test
    @DisplayName("o texto para o mapa sai completo, sem o complemento")
    void formatado() {
        Address a = new Address("87020-900", "Av. Colombo", "5790", "Bloco A", "Zona 7", "Maringá", "pr");

        // "Bloco A" confunde a busca dos apps de rota e não muda o ponto no mapa.
        assertThat(a.formatted()).isEqualTo("Av. Colombo, 5790 - Zona 7, Maringá - PR, 87020-900");
    }

    @Test
    @DisplayName("sem numero, sem bairro e sem CEP ainda forma um texto que o mapa entende")
    void formatadoIncompleto() {
        assertThat(new Address(null, "Rod. PR-317", null, null, null, "Maringá", "PR").formatted())
                .isEqualTo("Rod. PR-317, Maringá - PR");
    }

    @Test
    @DisplayName("endereco so com campos em branco vira nulo, e nao um objeto que parece preenchido")
    void vazioViraNulo() {
        assertThat(new AddressDTO(" ", "", null, null, " ", null, "", null).toAddress()).isNull();
        assertThat(AddressDTO.from(new Address())).isNull();
    }

    @Test
    @DisplayName("UF volta em maiusculas e os espacos saem")
    void limpa() {
        Address a = new AddressDTO(" 87020-900 ", " Av. Colombo ", "5790", null, null, "Maringá", "pr", null).toAddress();

        assertThat(a.getState()).isEqualTo("PR");
        assertThat(a.getStreet()).isEqualTo("Av. Colombo");
        assertThat(a.isUsable()).isTrue();
    }

    @Test
    @DisplayName("sem rua ou sem cidade nao serve para o mapa")
    void usavel() {
        assertThat(new Address(null, "Av. Colombo", null, null, null, null, null).isUsable()).isFalse();
        assertThat(new Address(null, null, null, null, null, "Maringá", null).isUsable()).isFalse();
    }

    @Test
    @DisplayName("o ponto do mapa vai e volta inteiro")
    void coordenadas() {
        AddressDTO enviado = new AddressDTO(null, "R. Néo Alves Martins", "2100", null, "Zona 01", "Maringá", "PR",
                new BigDecimal("-23.422847"), new BigDecimal("-51.932050"), null);

        Address a = enviado.toAddress();
        assertThat(a.hasCoordinates()).isTrue();

        AddressDTO devolvido = AddressDTO.from(a);
        assertThat(devolvido.latitude()).isEqualByComparingTo("-23.422847");
        assertThat(devolvido.longitude()).isEqualByComparingTo("-51.932050");
    }

    @Test
    @DisplayName("meia coordenada e descartada, e nao gravada sozinha")
    void meiaCoordenada() {
        // O banco recusaria (CHECK da V107), mas o 500 sairia lá de dentro, sem
        // dizer qual campo. Aqui o endereço só perde o ponto e segue salvando.
        Address a = new AddressDTO(null, "R. Néo Alves Martins", "2100", null, null, "Maringá", "PR",
                new BigDecimal("-23.422847"), null, null).toAddress();

        assertThat(a.hasCoordinates()).isFalse();
        assertThat(a.getLatitude()).isNull();
        assertThat(a.isUsable()).isTrue();
    }

    @Test
    @DisplayName("coordenada sem endereco nenhum nao cria um endereco")
    void coordenadaOrfaVirandoNulo() {
        assertThat(new AddressDTO(null, null, null, null, null, null, null,
                new BigDecimal("-23.422847"), new BigDecimal("-51.932050"), null).toAddress()).isNull();
    }
}
