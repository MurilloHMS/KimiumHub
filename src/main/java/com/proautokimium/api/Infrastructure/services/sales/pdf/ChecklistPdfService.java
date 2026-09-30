package com.proautokimium.api.Infrastructure.services.sales.pdf;

import com.proautokimium.api.domain.entities.sales.Checklist;
import com.proautokimium.api.domain.enums.sales.ChecklistStatus;
import com.proautokimium.api.domain.valueObjects.BrazilianDocument;
import com.proautokimium.api.domain.valueObjects.sales.ChecklistContent;
import com.proautokimium.api.domain.valueObjects.sales.ChecklistContent.Address;
import com.proautokimium.api.domain.valueObjects.sales.ChecklistContent.Customer;
import com.proautokimium.api.Infrastructure.services.sales.pdf.PdfSheet.Field;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.text.NumberFormat;
import java.time.Clock;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * O comprovante do checklist, com o desenho da planilha
 * "CHECK LIST - MODELO.xlsx": as mesmas seções, na mesma ordem, com os mesmos
 * títulos. É o que o vendedor mostra ao cliente e o que a Controladoria usa
 * para lançar no Sankhya.
 */
@Service
public class ChecklistPdfService {

    private static final DateTimeFormatter STAMP = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
    private static final Locale PT_BR = Locale.of("pt", "BR");

    private final Clock clock;

    public ChecklistPdfService(Clock clock) {
        this.clock = clock;
    }

    public byte[] generate(Checklist checklist) {
        ChecklistContent content = checklist.getContent();
        String number = checklist.getNumber() == null ? "—" : String.format("%04d", checklist.getNumber());
        String footer = "KimiumHub · Checklist nº " + number + " · versão " + checklist.getVersion()
                + " · gerado em " + LocalDateTime.now(clock).format(STAMP);

        try (PdfSheet pdf = new PdfSheet(footer)) {
            pdf.header("CHECKLIST DE VENDAS — Nº " + number,
                    status(checklist.getStatus()) + " · versão " + checklist.getVersion()
                            + " · enviado em " + checklist.getLastSubmittedAt().format(STAMP)
                            + (checklist.isFilledOffline() ? " · preenchido sem internet" : ""),
                    "Para liberação de pedido de venda, comodato de equipamentos e máquinas é necessário enviar "
                            + "previamente as informações abaixo. Todas serão inseridas no cadastro do cliente.");

            customer(pdf, checklist, content.customer());
            addresses(pdf, content);
            contract(pdf, content.customer());
            installation(pdf, content.installation());
            comodato(pdf, content.comodato());
            visual(pdf, content.visual());
            order(pdf, content.order());
            review(pdf, checklist);
            return pdf.finish();
        }
    }

    // Cada seção é o bloco da planilha, na mesma ordem e com o mesmo título.
    // A arrumação é para caber numa página (pedido dele, 2026-09-30): linhas
    // de quatro campos, e seção vazia vira uma frase em vez de uma tabela.

    private void customer(PdfSheet pdf, Checklist checklist, Customer c) {
        pdf.section("Cliente");
        String code = c.code() == null || c.newCustomer() ? "Cliente novo" : String.valueOf(c.code());
        pdf.fields(Field.of("Código / cliente", code + " — " + c.name(), 2.6f),
                Field.of("Razão social", c.legalName(), 2.2f),
                Field.of("Solicitante / vendedor", checklist.getSellerName(), 1.6f));
    }

    private void addresses(PdfSheet pdf, ChecklistContent content) {
        pdf.section("Endereços");
        address(pdf, "Endereço principal", content.mainAddress());
        ChecklistContent.UnitContact u = content.unitContact();
        String delivery = Boolean.FALSE.equals(content.deliverySameAsMain())
                ? oneLine(content.deliveryAddress()) : "O mesmo endereço principal";
        pdf.fields(Field.of("Endereço de entrega", delivery, 3),
                Field.of("Contato da unidade", u == null ? null : u.name(), 1.4f),
                Field.of("Horário de recebimento", u == null ? null : u.receivingHours(), 1.2f),
                Field.of("Telefone", u == null ? null : phone(u.phone()), 1.1f));
    }

    private void address(PdfSheet pdf, String label, Address a) {
        if (a == null) {
            pdf.fields(Field.of(label, null));
            return;
        }
        String street = join(", ", a.street(), a.number(), a.complement());
        pdf.fields(Field.of(label, street, 3), Field.of("Bairro", a.district(), 1.6f),
                Field.of("Cidade", a.city(), 1.6f), Field.of("Estado", a.state(), 0.6f),
                Field.of("CEP", zip(a.zipCode()), 0.9f));
    }

    private static String oneLine(Address a) {
        if (a == null) return null;
        return join(", ", a.street(), a.number(), a.complement()) + " — " + join(", ", a.district(),
                join("/", a.city(), a.state())) + (a.zipCode() == null ? "" : " — CEP " + zip(a.zipCode()));
    }

    private void contract(PdfSheet pdf, Customer c) {
        pdf.section("Dados cadastrais / contrato");
        pdf.fields(Field.of("Cartão CNPJ", document(c.document())),
                Field.of("Inscrição estadual (Sintegra)", c.stateRegistration()),
                Field.of("Telefone principal", phone(c.mainPhone())),
                Field.of("Celular", phone(c.mobile())));
        pdf.fields(Field.of("Responsável assinatura", c.signatory()),
                Field.of("CPF", document(c.signatoryCpf())),
                Field.of("E-mail para envio de NFs", c.invoiceEmail()),
                Field.of("E-mail envio do contrato", c.contractEmail()));
    }

    private void installation(PdfSheet pdf, ChecklistContent.Installation i) {
        pdf.section("Informações sobre instalação e pedido");
        boolean machines = i != null && Boolean.TRUE.equals(i.needsMachine()) && !i.machines().isEmpty();
        pdf.fields(Field.of("Pedido irá com a manutenção?", yesNo(i == null ? null : i.withMaintenance())),
                Field.of("Será necessária máquina no cliente?", yesNo(i == null ? null : i.needsMachine())),
                Field.of("Observação", i == null ? null : i.notes(), 2));
        if (machines) {
            List<String[]> rows = new ArrayList<>();
            int n = 1;
            for (ChecklistContent.Machine m : i.machines()) {
                String type = "OUTRA".equals(m.type()) ? "Outra: " + m.otherType() : machineType(m.type());
                rows.add(new String[]{"Máquina " + n++, type, String.valueOf(m.quantity()), yesNo(m.withTable())});
            }
            pdf.table(new String[]{"Máquinas lavadoras", "Tipo", "Qtd", "Acompanha mesa?"},
                    new float[]{1.4f, 2.6f, 0.6f, 1.4f}, rows);
        }
    }

    private void comodato(PdfSheet pdf, ChecklistContent.Comodato c) {
        pdf.section("Equipamentos em comodato");
        List<String[]> rows = new ArrayList<>();
        if (c != null) {
            for (ChecklistContent.ComodatoItem item : c.items()) {
                rows.add(new String[]{String.valueOf(item.productCode()),
                        item.name() + (item.popularName() == null ? "" : " (" + item.popularName() + ")"),
                        String.valueOf(item.quantity())});
            }
            for (ChecklistContent.ExtraItem item : c.extraItems()) {
                rows.add(new String[]{"—", item.description() + " (fora da lista)", String.valueOf(item.quantity())});
            }
        }
        if (rows.isEmpty()) {
            pdf.fields(Field.of("Equipamentos", "Nenhum equipamento em comodato"));
        } else {
            pdf.table(new String[]{"Código", "Equipamento", "Qtd"}, new float[]{0.7f, 5.4f, 0.6f}, rows);
        }
        if (c != null && c.notes() != null && !c.notes().isBlank()) {
            pdf.fields(Field.of("Observações", c.notes()));
        }
    }

    private void visual(PdfSheet pdf, ChecklistContent.Visual v) {
        pdf.section("Comunicação visual e diluição de implantação dos produtos");
        String items = v == null ? "" : String.join(" · ", v.items().stream().filter(i -> i.quantity() > 0)
                .map(i -> i.name() + " (" + i.quantity() + ")").toList());
        Boolean docs = v == null ? null : v.technicalDocs();
        pdf.fields(Field.of("Comunicação visual", items.isBlank() ? "Nenhum item" : items, 3),
                Field.of("Documentação técnica digital (boletim e FISPQ)?",
                        Boolean.TRUE.equals(docs) ? "Sim — " + v.technicalDocsEmail() : yesNo(docs), 2));
        if (v != null && !v.products().isEmpty()) {
            List<String[]> products = new ArrayList<>();
            for (ChecklistContent.UsedProduct p : v.products()) {
                products.add(new String[]{p.name(), yesNo(p.equipmentLabel()), yesNo(p.bottleLabel()), p.dilution()});
            }
            pdf.table(new String[]{"Produtos utilizados", "Etiqueta equipamentos", "Etiqueta frasco", "Diluição"},
                    new float[]{3.4f, 1.2f, 1f, 1.2f}, products);
        }
    }

    private void order(PdfSheet pdf, ChecklistContent.Order o) {
        boolean enabled = o != null && o.enabled();
        String kind = !enabled ? "Sem pedido"
                : "VENDA [" + ("VENDA".equals(o.kind()) ? "X" : " ") + "]    BONIFICADO ["
                  + ("BONIFICADO".equals(o.kind()) ? "X" : " ") + "]";
        pdf.section("Pedido: " + kind);
        if (!enabled) {
            return;
        }
        List<String[]> rows = new ArrayList<>();
        for (ChecklistContent.OrderItem item : o.items()) {
            String unit = item.unit() == null ? "" : item.unit();
            BigDecimal packagePrice = item.packageSize() == null || item.unitPrice() == null ? null
                    : item.packageSize().multiply(item.unitPrice());
            // A tabela numa coluna própria, e não embaixo do nome: assim cada
            // item é uma linha, e o pedido cabe na página.
            String table = item.priceTable() == null ? "—"
                    : item.priceTable() + ("CLIENTE".equals(item.priceSource()) ? " cliente" : "");
            rows.add(new String[]{
                    item.name(),
                    String.valueOf(item.packages()),
                    decimal(item.packageSize()) + " " + unit,
                    money(item.unitPrice()) + " / " + unit,
                    table,
                    decimal(item.ipiPercent()) + "%",
                    money(packagePrice),
                    money(item.lineTotal())});
        }
        pdf.table(new String[]{"Produto", "Qtd", "Embalagem", "Preço s/ imposto", "Tabela", "IPI", "Preço embalagem", "Preço total"},
                new float[]{3.3f, 0.5f, 0.9f, 1.2f, 0.9f, 0.6f, 1.1f, 1.1f}, rows, 18);
        pdf.total("Total", money(o.total()));
    }

    private void review(PdfSheet pdf, Checklist c) {
        if (c.getReviewedAt() == null && c.getChangeReason() == null) {
            return;
        }
        pdf.section("Controladoria");
        if (c.getReviewedAt() != null) {
            pdf.fields(Field.of("Situação", status(c.getStatus())),
                    Field.of("Analisado em", c.getReviewedAt().format(STAMP)),
                    Field.of("Observação", c.getReviewNotes(), 2));
        }
        if (c.getStatus() == ChecklistStatus.CHANGE_REQUESTED) {
            pdf.fields(Field.of("Alteração pedida pelo vendedor", c.getChangeReason()));
        }
    }

    // ── Formatação ───────────────────────────────────────────────────────────

    static String status(ChecklistStatus status) {
        return switch (status) {
            case SUBMITTED -> "Aguardando análise";
            case APPROVED -> "Aprovado";
            case RETURNED -> "Devolvido para correção";
            case CHANGE_REQUESTED -> "Alteração solicitada";
            case REOPENED -> "Liberado para alteração";
        };
    }

    private static String machineType(String type) {
        if (type == null) return null;
        return switch (type) {
            case "CAPO" -> "Capô";
            case "ESTEIRA" -> "Esteira";
            case "FRONTAL" -> "Frontal";
            default -> type;
        };
    }

    private static String yesNo(Boolean value) {
        return value == null ? null : value ? "Sim" : "Não";
    }

    private static String money(BigDecimal value) {
        return value == null ? "—" : NumberFormat.getCurrencyInstance(PT_BR).format(value).replace(' ', ' ');
    }

    private static String decimal(BigDecimal value) {
        if (value == null) return "—";
        NumberFormat f = NumberFormat.getNumberInstance(PT_BR);
        f.setMaximumFractionDigits(4);
        return f.format(value);
    }

    static String document(String value) {
        String d = BrazilianDocument.digits(value);
        if (d.length() == 14) {
            return d.replaceFirst("(\\d{2})(\\d{3})(\\d{3})(\\d{4})(\\d{2})", "$1.$2.$3/$4-$5");
        }
        if (d.length() == 11) {
            return d.replaceFirst("(\\d{3})(\\d{3})(\\d{3})(\\d{2})", "$1.$2.$3-$4");
        }
        return value;
    }

    static String phone(String value) {
        String d = BrazilianDocument.digits(value);
        if (d.length() == 11) return d.replaceFirst("(\\d{2})(\\d{5})(\\d{4})", "($1) $2-$3");
        if (d.length() == 10) return d.replaceFirst("(\\d{2})(\\d{4})(\\d{4})", "($1) $2-$3");
        return value;
    }

    private static String zip(String value) {
        String d = BrazilianDocument.digits(value);
        return d.length() == 8 ? d.substring(0, 5) + "-" + d.substring(5) : value;
    }

    private static String join(String separator, String... parts) {
        List<String> present = new ArrayList<>();
        for (String p : parts) {
            if (p != null && !p.isBlank()) present.add(p.strip());
        }
        return String.join(separator, present);
    }
}
