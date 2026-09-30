package com.proautokimium.api.domain.valueObjects.sales;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

/**
 * O que o vendedor preencheu: as oito etapas do checklist, na ordem da planilha.
 *
 * É um documento, e vai inteiro para a coluna {@code payload} (jsonb). O
 * formato é o mesmo que o celular guarda e envia, e cada envio aceito fica
 * congelado em {@code checklist_versions} — é de lá que o histórico tira o
 * "antes".
 *
 * Texto que tem lista fechada (tipo de máquina, tipo de pedido) viaja como
 * texto, e não como enum: um valor desconhecido vindo de um celular com o site
 * antigo vira uma frase na validação, e não um 500 na leitura do JSON.
 *
 * Campo desconhecido é ignorado, pelo mesmo motivo: um celular com o site de
 * antes de uma mudança (o "tipo da mesa" saiu em 2026-09-30) ainda manda o
 * campo, e o leitor de JSON do Hibernate recusaria o documento inteiro.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record ChecklistContent(
        Customer customer,
        Address mainAddress,
        Boolean deliverySameAsMain,
        Address deliveryAddress,
        UnitContact unitContact,
        Installation installation,
        Comodato comodato,
        Visual visual,
        Order order
) {

    // ── Etapas 1 e 3: o cliente e os dados do contrato ───────────────────────

    /**
     * @param code      CODPARC do Sankhya; nulo para cliente novo
     * @param erp       o que o Sankhya tinha em cada campo quando o vendedor
     *                  escolheu o cliente — é assim que a Controladoria vê
     *                  "Sankhya: X → checklist: Y" sem consultar o ERP de novo
     */
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Customer(
            Integer code,
            boolean newCustomer,
            String name,
            String legalName,
            String document,
            String stateRegistration,
            String mainPhone,
            String mobile,
            String signatory,
            String signatoryCpf,
            String invoiceEmail,
            String contractEmail,
            Integer priceTable,
            Map<String, String> erp
    ) {}

    // ── Etapa 2: endereços e contato da unidade ──────────────────────────────

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Address(
            String zipCode,
            String street,
            String number,
            String complement,
            String district,
            String city,
            String state
    ) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record UnitContact(String name, String receivingHours, String phone) {}

    // ── Etapa 4: instalação e máquinas ───────────────────────────────────────

    public static final List<String> MACHINE_TYPES = List.of("CAPO", "ESTEIRA", "FRONTAL", "OUTRA");

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Installation(Boolean withMaintenance, Boolean needsMachine, List<Machine> machines, String notes) {
        public List<Machine> machines() { return machines == null ? List.of() : machines; }
    }

    /**
     * {@code otherType} só quando o tipo é OUTRA. "Vai com mesa?" é só Sim ou
     * Não: o tipo da mesa saiu a pedido dele (2026-09-30).
     */
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Machine(String type, String otherType, int quantity, Boolean withTable) {}

    // ── Etapa 5: comodato ────────────────────────────────────────────────────

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Comodato(List<ComodatoItem> items, List<ExtraItem> extraItems, String notes) {
        public List<ComodatoItem> items() { return items == null ? List.of() : items; }
        public List<ExtraItem> extraItems() { return extraItems == null ? List.of() : extraItems; }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record ComodatoItem(int productCode, String name, String popularName, int quantity) {}

    /** O "não achei na lista": texto livre, como a planilha tinha. */
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record ExtraItem(String description, int quantity) {}

    // ── Etapa 6: comunicação visual e técnica ────────────────────────────────

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Visual(List<VisualItem> items, List<UsedProduct> products,
                         Boolean technicalDocs, String technicalDocsEmail) {
        public List<VisualItem> items() { return items == null ? List.of() : items; }
        public List<UsedProduct> products() { return products == null ? List.of() : products; }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record VisualItem(UUID itemId, String name, int quantity) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record UsedProduct(int productCode, String name, boolean equipmentLabel,
                              boolean bottleLabel, String dilution) {}

    // ── Etapa 7: pedido (opcional) ───────────────────────────────────────────

    public static final List<String> ORDER_KINDS = List.of("VENDA", "BONIFICADO");

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Order(boolean enabled, String kind, List<OrderItem> items, BigDecimal total) {
        public List<OrderItem> items() { return items == null ? List.of() : items; }
    }

    /**
     * Uma linha do pedido. O preço é por {@code unit} (KG, LT), como a tabela do
     * Sankhya guarda; a quantidade é em embalagens ({@code packages} ×
     * {@code packageSize}). {@code priceTable} é a tabela de onde o preço saiu,
     * congelada no momento do preenchimento.
     */
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record OrderItem(int productCode, String name, String unit, BigDecimal packageSize,
                            String packageLabel, int packages, BigDecimal unitPrice,
                            BigDecimal ipiPercent, Integer priceTable, String priceSource,
                            BigDecimal lineTotal) {

        /** embalagens × tamanho × preço, mais o IPI — a conta da planilha. */
        public BigDecimal computedTotal() {
            if (packageSize == null || unitPrice == null) {
                return BigDecimal.ZERO;
            }
            BigDecimal base = packageSize.multiply(unitPrice).multiply(BigDecimal.valueOf(packages));
            BigDecimal ipi = ipiPercent == null ? BigDecimal.ZERO : ipiPercent;
            return base.multiply(BigDecimal.ONE.add(ipi.movePointLeft(2))).setScale(2, RoundingMode.HALF_UP);
        }

        OrderItem withComputedTotal() {
            return new OrderItem(productCode, name, unit, packageSize, packageLabel, packages, unitPrice,
                    ipiPercent, priceTable, priceSource, computedTotal());
        }
    }

    public boolean hasOrder() {
        return order != null && order.enabled();
    }

    /**
     * O mesmo conteúdo, com as contas do pedido refeitas aqui. O celular mostra
     * o total, mas quem grava é o servidor: um arredondamento diferente no
     * aparelho não pode virar o valor do comprovante.
     */
    public ChecklistContent withRecomputedOrder() {
        if (order == null) {
            return this;
        }
        List<OrderItem> items = order.items().stream().map(OrderItem::withComputedTotal).toList();
        BigDecimal total = items.stream().map(OrderItem::lineTotal)
                .filter(Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .setScale(2, RoundingMode.HALF_UP);
        Order recomputed = new Order(order.enabled(), order.kind(), items, order.enabled() ? total : null);
        return new ChecklistContent(customer, mainAddress, deliverySameAsMain, deliveryAddress, unitContact,
                installation, comodato, visual, recomputed);
    }
}
