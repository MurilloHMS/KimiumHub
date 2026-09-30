package com.proautokimium.api.domain.valueObjects.sales;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/** Um checklist válido de exemplo, e variações dele. Documentos de teste, não de gente. */
public final class ChecklistFixtures {

    public static final String CNPJ = "11222333000181";
    public static final String CPF = "52998224725";

    private ChecklistFixtures() {
    }

    public static ChecklistContent.Customer customer() {
        return new ChecklistContent.Customer(3661, false, "Mercado Central - Unid. 2", "Mercado Central Ltda",
                CNPJ, "ISENTO", "1932345678", "19987654321", "Maria Aparecida Souza", CPF,
                "financeiro@mercado.com.br", "contrato@mercado.com.br", 80,
                Map.of("name", "MERCADO CENTRAL - UNID. 2", "district", "VILA INDUSTRIAL",
                        "zipCode", "13015904", "city", "CAMPINAS"));
    }

    public static ChecklistContent.Address address() {
        return new ChecklistContent.Address("13015-904", "Avenida Francisco Glicério", "1200", null,
                "Centro", "Campinas", "SP");
    }

    public static ChecklistContent valid() {
        return new ChecklistContent(
                customer(),
                address(),
                true,
                null,
                new ChecklistContent.UnitContact("Jorge", "8h às 17h", "1932345678"),
                new ChecklistContent.Installation(false, true,
                        List.of(new ChecklistContent.Machine("CAPO", null, 1, true)), null),
                new ChecklistContent.Comodato(
                        List.of(new ChecklistContent.ComodatoItem(1998, "DILUIDOR NTI - AZUL", "Diluidor padrão", 2)),
                        List.of(), null),
                new ChecklistContent.Visual(
                        List.of(new ChecklistContent.VisualItem(UUID.randomUUID(), "Lave sempre as mãos", 3)),
                        List.of(), false, null),
                order());
    }

    public static ChecklistContent.Order order() {
        return new ChecklistContent.Order(true, "VENDA", List.of(
                new ChecklistContent.OrderItem(197, "PROAUTO REMOCON. - 20 LT BB PRETA", "LT",
                        new BigDecimal("20"), "20 LT", 3, new BigDecimal("10.98"), new BigDecimal("3.25"),
                        281, "CLIENTE", null),
                new ChecklistContent.OrderItem(455, "POSEIDON - 7,5 KG GL NATURAL", "KG",
                        new BigDecimal("7.5"), "7,5 KG GL", 2, new BigDecimal("35.31"), BigDecimal.ZERO,
                        80, "GERAL", null)), null);
    }

    public static ChecklistContent withCustomer(ChecklistContent c, ChecklistContent.Customer customer) {
        return new ChecklistContent(customer, c.mainAddress(), c.deliverySameAsMain(), c.deliveryAddress(),
                c.unitContact(), c.installation(), c.comodato(), c.visual(), c.order());
    }

    public static ChecklistContent withAddress(ChecklistContent c, ChecklistContent.Address address) {
        return new ChecklistContent(c.customer(), address, c.deliverySameAsMain(), c.deliveryAddress(),
                c.unitContact(), c.installation(), c.comodato(), c.visual(), c.order());
    }

    public static ChecklistContent withComodato(ChecklistContent c, ChecklistContent.Comodato comodato) {
        return new ChecklistContent(c.customer(), c.mainAddress(), c.deliverySameAsMain(), c.deliveryAddress(),
                c.unitContact(), c.installation(), comodato, c.visual(), c.order());
    }
}
