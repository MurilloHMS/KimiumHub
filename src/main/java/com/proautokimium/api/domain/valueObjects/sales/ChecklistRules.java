package com.proautokimium.api.domain.valueObjects.sales;

import com.proautokimium.api.domain.valueObjects.BrazilianDocument;
import com.proautokimium.api.domain.valueObjects.Email;
import com.proautokimium.api.domain.valueObjects.sales.ChecklistContent.Address;
import com.proautokimium.api.domain.valueObjects.sales.ChecklistContent.Customer;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

/**
 * O que um checklist precisa ter para ser enviado.
 *
 * Obrigatórios, por decisão dele (2026-09-29): o endereço e os dados
 * cadastrais / contrato. O resto é o que a própria planilha já exigia (as
 * perguntas de Sim/Não da instalação, a quantidade de cada item) e o pedido,
 * que é opcional mas, quando existe, precisa fechar a conta.
 *
 * Devolve frases, e não códigos: cada uma diz a etapa e o que fazer, porque é
 * isso que o vendedor lê. Lista vazia = pode enviar.
 */
public final class ChecklistRules {

    private ChecklistRules() {
    }

    public static List<String> problems(ChecklistContent content) {
        List<String> problems = new ArrayList<>();
        if (content == null) {
            problems.add("O checklist chegou vazio.");
            return problems;
        }

        customer(content.customer(), problems);
        address("Etapa 2 — endereço principal", content.mainAddress(), problems);
        if (content.deliverySameAsMain() == null) {
            problems.add("Etapa 2 — responda se a entrega é no mesmo endereço.");
        } else if (!content.deliverySameAsMain()) {
            address("Etapa 2 — endereço de entrega", content.deliveryAddress(), problems);
        }
        installation(content.installation(), problems);
        comodato(content.comodato(), problems);
        visual(content.visual(), problems);
        order(content.order(), problems);
        return problems;
    }

    private static void customer(Customer c, List<String> problems) {
        if (c == null) {
            problems.add("Etapa 1 — escolha o cliente ou cadastre um cliente novo.");
            return;
        }
        if (blank(c.name())) {
            problems.add("Etapa 1 — informe o nome do cliente.");
        }
        if (!BrazilianDocument.isValidCpfOrCnpj(c.document())) {
            problems.add("Etapa 3 — o CNPJ (ou CPF) do cliente é inválido.");
        }
        if (blank(c.stateRegistration())) {
            problems.add("Etapa 3 — informe a inscrição estadual (ou ISENTO).");
        }
        if (!phone(c.mainPhone())) {
            problems.add("Etapa 3 — informe o telefone principal com DDD.");
        }
        if (!phone(c.mobile())) {
            problems.add("Etapa 3 — informe o celular com DDD.");
        }
        if (blank(c.signatory())) {
            problems.add("Etapa 3 — informe quem assina o contrato.");
        } else if (!fullName(c.signatory())) {
            problems.add("Etapa 3 — escreva o nome e o sobrenome de quem assina.");
        }
        if (!BrazilianDocument.isValidCpf(c.signatoryCpf())) {
            problems.add("Etapa 3 — o CPF de quem assina é inválido.");
        }
        if (!Email.isValid(trim(c.invoiceEmail()))) {
            problems.add("Etapa 3 — o e-mail para as notas fiscais é inválido.");
        }
        if (!Email.isValid(trim(c.contractEmail()))) {
            problems.add("Etapa 3 — o e-mail para o contrato é inválido.");
        }
    }

    private static void address(String where, Address a, List<String> problems) {
        if (a == null) {
            problems.add(where + ": preencha o endereço.");
            return;
        }
        if (BrazilianDocument.digits(a.zipCode()).length() != 8) {
            problems.add(where + ": o CEP precisa ter 8 números.");
        }
        if (blank(a.street())) {
            problems.add(where + ": informe a rua.");
        }
        if (blank(a.number())) {
            problems.add(where + ": informe o número (ou S/N).");
        }
        if (blank(a.district())) {
            problems.add(where + ": informe o bairro.");
        }
        if (blank(a.city())) {
            problems.add(where + ": informe a cidade.");
        }
        if (a.state() == null || !a.state().trim().matches("[A-Za-z]{2}")) {
            problems.add(where + ": informe o estado (UF), como SP.");
        }
    }

    private static void installation(ChecklistContent.Installation i, List<String> problems) {
        if (i == null || i.withMaintenance() == null) {
            problems.add("Etapa 4 — responda se o pedido vai com a manutenção.");
        }
        if (i == null || i.needsMachine() == null) {
            problems.add("Etapa 4 — responda se vai precisar de máquina no cliente.");
            return;
        }
        if (!i.needsMachine()) {
            return;
        }
        if (i.machines().isEmpty()) {
            problems.add("Etapa 4 — informe qual máquina vai para o cliente.");
        }
        for (int n = 0; n < i.machines().size(); n++) {
            ChecklistContent.Machine m = i.machines().get(n);
            String where = "Etapa 4 — máquina " + (n + 1);
            if (m.type() == null || !ChecklistContent.MACHINE_TYPES.contains(m.type())) {
                problems.add(where + ": escolha o tipo.");
            } else if ("OUTRA".equals(m.type()) && blank(m.otherType())) {
                problems.add(where + ": escreva qual é a máquina.");
            }
            if (m.quantity() < 1) {
                problems.add(where + ": a quantidade precisa ser pelo menos 1.");
            }
            if (m.withTable() == null) {
                problems.add(where + ": responda se vai com mesa.");
            }
        }
    }

    private static void comodato(ChecklistContent.Comodato c, List<String> problems) {
        if (c == null) {
            return;
        }
        for (ChecklistContent.ComodatoItem item : c.items()) {
            if (item.quantity() < 1) {
                problems.add("Etapa 5 — a quantidade de \"" + label(item.popularName(), item.name())
                        + "\" precisa ser pelo menos 1.");
            }
        }
        for (ChecklistContent.ExtraItem item : c.extraItems()) {
            if (blank(item.description())) {
                problems.add("Etapa 5 — escreva o nome do item que não estava na lista.");
            } else if (item.quantity() < 1) {
                problems.add("Etapa 5 — a quantidade de \"" + item.description().trim()
                        + "\" precisa ser pelo menos 1.");
            }
        }
    }

    private static void visual(ChecklistContent.Visual v, List<String> problems) {
        if (v == null) {
            return;
        }
        for (ChecklistContent.VisualItem item : v.items()) {
            if (item.quantity() < 0) {
                problems.add("Etapa 6 — a quantidade de \"" + item.name() + "\" não pode ser negativa.");
            }
        }
        if (Boolean.TRUE.equals(v.technicalDocs()) && !Email.isValid(trim(v.technicalDocsEmail()))) {
            problems.add("Etapa 6 — informe o e-mail para a documentação técnica.");
        }
    }

    private static void order(ChecklistContent.Order o, List<String> problems) {
        if (o == null || !o.enabled()) {
            return;
        }
        if (o.kind() == null || !ChecklistContent.ORDER_KINDS.contains(o.kind())) {
            problems.add("Etapa 7 — escolha se o pedido é venda ou bonificado.");
        }
        if (o.items().isEmpty()) {
            problems.add("Etapa 7 — o pedido precisa de pelo menos um produto (ou marque \"sem pedido\").");
        }
        for (ChecklistContent.OrderItem item : o.items()) {
            String name = "\"" + (item.name() == null ? item.productCode() : item.name()) + "\"";
            if (item.packages() < 1) {
                problems.add("Etapa 7 — a quantidade de " + name + " precisa ser pelo menos 1.");
            }
            if (item.packageSize() == null || item.packageSize().signum() <= 0) {
                problems.add("Etapa 7 — informe o tamanho da embalagem de " + name + ".");
            }
            if (item.unitPrice() == null || item.unitPrice().compareTo(BigDecimal.ZERO) < 0) {
                problems.add("Etapa 7 — " + name + " está sem preço.");
            }
        }
    }

    /**
     * Nome e sobrenome: pelo menos duas palavras com duas letras ou mais
     * ("Maria Souza"). Pedido dele (2026-09-30): o contrato não sai com um nome
     * só. "Maria S." não passa — a inicial não identifica ninguém.
     */
    static boolean fullName(String value) {
        if (value == null) {
            return false;
        }
        long words = java.util.Arrays.stream(value.trim().split("\\s+"))
                .filter(w -> w.replaceAll("[^\\p{L}]", "").length() >= 2)
                .count();
        return words >= 2;
    }

    private static boolean phone(String value) {
        int length = BrazilianDocument.digits(value).length();
        return length == 10 || length == 11;
    }

    private static boolean blank(String value) {
        return value == null || value.isBlank();
    }

    private static String trim(String value) {
        return value == null ? null : value.trim();
    }

    private static String label(String preferred, String fallback) {
        return blank(preferred) ? fallback : preferred;
    }
}
