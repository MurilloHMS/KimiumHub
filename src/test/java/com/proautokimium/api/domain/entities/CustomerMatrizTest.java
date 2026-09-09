package com.proautokimium.api.domain.entities;

import com.proautokimium.api.Application.DTOs.partners.CustomerRequestDTO;
import com.proautokimium.api.domain.valueObjects.Email;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

/**
 * Quem é matriz.
 *
 * <p><b>A regra:</b> matriz é quando {@code cod_parceiro = codigo_matriz}. É o
 * que o ERP faz — no {@code TGFPAR}, o {@code CODPARCMATRIZ} da matriz aponta
 * para o próprio {@code CODPARC} — e é o que o código já assumia:
 * {@code ClientAccessService.visibleUnits()} busca as unidades pelo próprio
 * código e depois <b>filtra a própria linha do resultado</b>. Esse filtro só faz
 * sentido porque a matriz se inclui na busca.
 *
 * <p>Antes disto o campo vinha de um checkbox do formulário — um segundo lugar
 * guardando a mesma verdade, sem nada obrigando os dois a concordarem. Estavam
 * <b>todos em false</b>: 7471 de 7471. Depois da V101 são 4623 matrizes, 62% da
 * base, contra 58% no Sankhya.
 */
class CustomerMatrizTest {

    private static CustomerRequestDTO dto(String codParceiro, String codMatriz) {
        return new CustomerRequestDTO(codParceiro, "12345678000199", "ACME",
                "acme@x.com", null, true, true, codMatriz, false);
    }

    @Test
    @DisplayName("código igual ao da matriz: é matriz")
    void codeEqualToGroupIsMatriz() {
        assertThat(Customer.fromDTO(dto("1708", "1708")).isMatriz()).isTrue();
    }

    @Test
    @DisplayName("código diferente: é unidade do grupo")
    void differentCodeIsUnit() {
        assertThat(Customer.fromDTO(dto("505", "1708")).isMatriz()).isFalse();
    }

    /**
     * <b>O checkbox deixou de mandar.</b> Este é o teste que segura a decisão:
     * o DTO diz {@code isMatriz = false} nos dois casos acima, e o resultado sai
     * do dado. Voltar a ler {@code dto.isMatriz()} faz o primeiro cair.
     */
    @Test
    @DisplayName("o isMatriz do DTO é ignorado — quem decide é o dado")
    void checkboxDoesNotDecide() {
        CustomerRequestDTO lying = new CustomerRequestDTO("505", "12345678000199",
                "ACME", "acme@x.com", null, true, true, "1708", /* isMatriz */ true);

        assertThat(Customer.fromDTO(lying).isMatriz())
                .as("o DTO afirma que é matriz, mas o código aponta para outro grupo")
                .isFalse();
    }

    /**
     * Existem linhas legadas com o código em branco ou nulo — duas foram
     * encontradas na produção. O construtor precisa atravessá-las sem estourar:
     * {@code null.equals(...)} é NullPointerException, e isso derrubaria a
     * criação de cliente inteira.
     */
    @Test
    @DisplayName("código nulo não estoura")
    void nullCodeDoesNotThrow() {
        assertThatCode(() -> Customer.fromDTO(dto(null, "1708"))).doesNotThrowAnyException();
        assertThatCode(() -> Customer.fromDTO(dto("1708", null))).doesNotThrowAnyException();

        assertThat(Customer.fromDTO(dto(null, "1708")).isMatriz()).isFalse();
        assertThat(Customer.fromDTO(dto("1708", null)).isMatriz()).isFalse();
    }

    /**
     * Sem código de matriz não há grupo, e sem grupo não há matriz.
     *
     * <p>É o mesmo recorte que a V101 fez no backfill
     * ({@code coalesce(codigo_matriz,'') <> ''}). Se o Java disser o contrário
     * do que a migration disse, a base passa a divergir da regra na primeira
     * edição.
     */
    @Test
    @DisplayName("sem código de matriz não é matriz, nem com o código também vazio")
    void noGroupIsNotMatriz() {
        assertThat(Customer.fromDTO(dto("", "")).isMatriz())
                .as("duas linhas legadas em branco viravam matriz de um grupo que não existe")
                .isFalse();

        assertThat(Customer.fromDTO(dto(null, null)).isMatriz()).isFalse();
    }

    @Test
    @DisplayName("o construtor guarda o que recebeu, fora o isMatriz")
    void constructorKeepsTheRest() {
        Customer c = new Customer("1708", "12345678000199", "ACME", null,
                new Email("acme@x.com"), true, true, "1708", false);

        assertThat(c.getCodParceiro()).isEqualTo("1708");
        assertThat(c.getCodigoMatriz()).isEqualTo("1708");
        assertThat(c.isRecebeEmail()).isTrue();
        assertThat(c.isMatriz())
                .as("o parâmetro diz false; a regra diz true")
                .isTrue();
    }
}
