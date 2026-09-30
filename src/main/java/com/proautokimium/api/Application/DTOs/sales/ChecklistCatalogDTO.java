package com.proautokimium.api.Application.DTOs.sales;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * Tudo que o celular guarda para o vendedor preencher sem internet.
 *
 * {@code version} muda quando qualquer parte muda (ERP ou cadastros daqui) e
 * vai no ETag: o aparelho só baixa de novo quando é diferente.
 */
public record ChecklistCatalogDTO(
        String version,
        LocalDateTime erpFetchedAt,
        List<Customer> customers,
        List<Product> products,
        List<Price> prices,
        List<ComodatoItem> comodato,
        List<VisualItem> visualItems
) {

    /** Um cliente ativo do Sankhya, com o endereço já montado. */
    public record Customer(int code, String name, String legalName, String document, String personType,
                           String stateRegistration, String phone, String invoiceEmail,
                           String zipCode, String street, String number, String complement,
                           String district, String city, String state, Integer priceTable) {}

    /**
     * Um produto. {@code usage} é o USOPROD (V venda, R revenda…): o pedido e
     * "produtos usados" mostram só V e R; os outros estão aqui por causa do
     * comodato. A embalagem vem do Sankhya quando existe, ou do nome
     * ({@code packageFromName}, para o vendedor conferir).
     */
    public record Product(int code, String name, String usage, long group, String unit, BigDecimal ipi,
                          String packageLabel, BigDecimal packageSize, boolean packageFromName) {}

    /** Preço por unidade (KG, LT) do produto na tabela vigente. */
    public record Price(int table, int product, BigDecimal price) {}

    /** {@code name} é o do Sankhya; {@code active} é falso se o ERP inativou. */
    public record ComodatoItem(UUID id, int productCode, String name, String popularName, boolean active) {}

    public record VisualItem(UUID id, String name) {}
}
