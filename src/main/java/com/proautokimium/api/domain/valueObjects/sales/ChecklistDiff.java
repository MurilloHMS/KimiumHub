package com.proautokimium.api.domain.valueObjects.sales;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/**
 * O que mudou de uma versão do checklist para a outra, campo a campo, com nome
 * de gente: "Dados do contrato › CPF de quem assina", e não
 * {@code customer.signatoryCpf}.
 *
 * <p>Item de lista é identificado pelo nome ("Comodato › Diluidor padrão ›
 * Quantidade"), e não pela posição: tirar o primeiro item não pode aparecer
 * como "todos os outros mudaram". O que não tem nome (máquina) vai pela posição.
 *
 * <p>Fica de fora o retrato do Sankhya ({@code customer.erp}), que não é
 * edição de ninguém, e os totais por linha, que são consequência da quantidade.
 */
public final class ChecklistDiff {

    public record Change(String field, String before, String after) {}

    private static final String SEP = " › ";

    private static final Set<String> SKIPPED = Set.of("erp", "lineTotal", "itemId");

    /**
     * Dentro de um item de lista, o que já está no caminho ("Comodato › Itens ›
     * Diluidor padrão") não se repete como campo: um item novo aparece como uma
     * linha ("Quantidade: — → 2"), e não quatro.
     */
    private static final Set<String> IDENTITY = Set.of("productCode", "name", "popularName", "description");

    private static final Map<String, String> LABELS = Map.ofEntries(
            // seções
            Map.entry("customer", "Cliente e contrato"),
            Map.entry("mainAddress", "Endereço principal"),
            Map.entry("deliverySameAsMain", "Entrega no mesmo endereço"),
            Map.entry("deliveryAddress", "Endereço de entrega"),
            Map.entry("unitContact", "Contato da unidade"),
            Map.entry("installation", "Instalação"),
            Map.entry("comodato", "Comodato"),
            Map.entry("visual", "Comunicação visual"),
            Map.entry("order", "Pedido"),
            // cliente
            Map.entry("code", "Código no Sankhya"),
            Map.entry("newCustomer", "Cliente novo"),
            Map.entry("name", "Nome"),
            Map.entry("legalName", "Razão social"),
            Map.entry("document", "CNPJ / CPF"),
            Map.entry("stateRegistration", "Inscrição estadual"),
            Map.entry("mainPhone", "Telefone principal"),
            Map.entry("mobile", "Celular"),
            Map.entry("signatory", "Quem assina o contrato"),
            Map.entry("signatoryCpf", "CPF de quem assina"),
            Map.entry("invoiceEmail", "E-mail das notas fiscais"),
            Map.entry("contractEmail", "E-mail do contrato"),
            Map.entry("priceTable", "Tabela de preço"),
            // endereço
            Map.entry("zipCode", "CEP"),
            Map.entry("street", "Rua"),
            Map.entry("number", "Número"),
            Map.entry("complement", "Complemento"),
            Map.entry("district", "Bairro"),
            Map.entry("city", "Cidade"),
            Map.entry("state", "Estado"),
            Map.entry("receivingHours", "Horário de recebimento"),
            Map.entry("phone", "Telefone"),
            // instalação
            Map.entry("withMaintenance", "Vai com a manutenção"),
            Map.entry("needsMachine", "Precisa de máquina"),
            Map.entry("machines", "Máquina"),
            Map.entry("type", "Tipo"),
            Map.entry("otherType", "Qual máquina"),
            Map.entry("quantity", "Quantidade"),
            Map.entry("withTable", "Vai com mesa"),
            Map.entry("notes", "Observação"),
            // comodato e visual
            Map.entry("items", "Itens"),
            Map.entry("extraItems", "Itens fora da lista"),
            Map.entry("popularName", "Nome popular"),
            Map.entry("productCode", "Código do produto"),
            Map.entry("description", "Descrição"),
            Map.entry("products", "Produtos usados"),
            Map.entry("equipmentLabel", "Etiqueta de equipamento"),
            Map.entry("bottleLabel", "Etiqueta de frasco"),
            Map.entry("dilution", "Diluição"),
            Map.entry("technicalDocs", "Documentação técnica por e-mail"),
            Map.entry("technicalDocsEmail", "E-mail da documentação técnica"),
            // pedido
            Map.entry("enabled", "Tem pedido"),
            Map.entry("kind", "Tipo do pedido"),
            Map.entry("unit", "Unidade"),
            Map.entry("packageSize", "Tamanho da embalagem"),
            Map.entry("packageLabel", "Embalagem"),
            Map.entry("packages", "Embalagens"),
            Map.entry("unitPrice", "Preço"),
            Map.entry("ipiPercent", "IPI (%)"),
            Map.entry("priceSource", "Origem do preço"),
            Map.entry("total", "Total")
    );

    private static final Map<String, String> VALUES = Map.ofEntries(
            Map.entry("CAPO", "Capô"),
            Map.entry("ESTEIRA", "Esteira"),
            Map.entry("FRONTAL", "Frontal"),
            Map.entry("OUTRA", "Outra"),
            Map.entry("VENDA", "Venda"),
            Map.entry("BONIFICADO", "Bonificado"),
            Map.entry("CLIENTE", "Tabela do cliente"),
            Map.entry("GERAL", "Tabela geral")
    );

    private ChecklistDiff() {
    }

    public static List<Change> between(ChecklistContent before, ChecklistContent after, ObjectMapper mapper) {
        Map<String, String> old = flatten(mapper.valueToTree(before));
        Map<String, String> now = flatten(mapper.valueToTree(after));

        Set<String> fields = new LinkedHashSet<>(old.keySet());
        fields.addAll(now.keySet());

        List<Change> changes = new ArrayList<>();
        for (String field : fields) {
            String a = old.getOrDefault(field, "");
            String b = now.getOrDefault(field, "");
            if (!Objects.equals(a, b)) {
                changes.add(new Change(field, a, b));
            }
        }
        return changes;
    }

    static Map<String, String> flatten(JsonNode root) {
        Map<String, String> out = new LinkedHashMap<>();
        if (root != null && !root.isNull()) {
            walk(root, "", out);
        }
        return out;
    }

    private static void walk(JsonNode node, String path, Map<String, String> out) {
        walk(node, path, out, false);
    }

    private static void walk(JsonNode node, String path, Map<String, String> out, boolean listItem) {
        if (node.isObject()) {
            Iterator<Map.Entry<String, JsonNode>> fields = node.fields();
            while (fields.hasNext()) {
                Map.Entry<String, JsonNode> field = fields.next();
                if (SKIPPED.contains(field.getKey()) || (listItem && IDENTITY.contains(field.getKey()))) {
                    continue;
                }
                walk(field.getValue(), join(path, label(field.getKey())), out, false);
            }
        } else if (node.isArray()) {
            for (int i = 0; i < node.size(); i++) {
                JsonNode item = node.get(i);
                walk(item, join(path, itemKey(item, i)), out, item.isObject());
            }
        } else {
            String value = value(node);
            if (!value.isEmpty()) {
                out.put(path, value);
            }
        }
    }

    /** O nome do item, quando ele tem um; senão, a posição ("1", "2"). */
    private static String itemKey(JsonNode item, int index) {
        for (String key : List.of("popularName", "name", "description")) {
            JsonNode value = item.get(key);
            if (value != null && value.isTextual() && !value.asText().isBlank()) {
                return value.asText().strip();
            }
        }
        return String.valueOf(index + 1);
    }

    private static String value(JsonNode node) {
        if (node.isNull() || node.isMissingNode()) {
            return "";
        }
        if (node.isBoolean()) {
            return node.asBoolean() ? "Sim" : "Não";
        }
        if (node.isNumber()) {
            return new BigDecimal(node.asText()).stripTrailingZeros().toPlainString();
        }
        String text = node.asText().strip();
        return VALUES.getOrDefault(text, text);
    }

    private static String label(String key) {
        return LABELS.getOrDefault(key, key);
    }

    private static String join(String path, String segment) {
        return path.isEmpty() ? segment : path + SEP + segment;
    }
}
