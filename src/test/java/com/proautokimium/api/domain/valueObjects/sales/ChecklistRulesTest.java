package com.proautokimium.api.domain.valueObjects.sales;

import com.proautokimium.api.domain.valueObjects.BrazilianDocument;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * O que é obrigatório para enviar. A mesma regra roda no celular antes de
 * enviar; esta é a do servidor, para um aparelho com site antigo não gravar
 * checklist incompleto.
 */
class ChecklistRulesTest {

    @Test
    @DisplayName("o checklist de exemplo passa")
    void validPasses() {
        assertThat(ChecklistRules.problems(ChecklistFixtures.valid())).isEmpty();
    }

    @Test
    @DisplayName("dados do contrato: CPF e CNPJ pelo dígito, e-mails e telefones com DDD")
    void contractIsRequired() {
        var c = ChecklistFixtures.customer();
        var broken = new ChecklistContent.Customer(c.code(), false, c.name(), null, "11222333000180", " ",
                "3234567", null, "", "12345678900", "financeiro@", null, 80, Map.of());

        List<String> problems = ChecklistRules.problems(ChecklistFixtures.withCustomer(ChecklistFixtures.valid(), broken));

        assertThat(problems).containsExactlyInAnyOrder(
                "Etapa 3 — o CNPJ (ou CPF) do cliente é inválido.",
                "Etapa 3 — informe a inscrição estadual (ou ISENTO).",
                "Etapa 3 — informe o telefone principal com DDD.",
                "Etapa 3 — informe o celular com DDD.",
                "Etapa 3 — informe quem assina o contrato.",
                "Etapa 3 — o CPF de quem assina é inválido.",
                "Etapa 3 — o e-mail para as notas fiscais é inválido.",
                "Etapa 3 — o e-mail para o contrato é inválido.");
    }

    @Test
    @DisplayName("endereço principal obrigatório: CEP de 8 números, rua, número, bairro, cidade e UF")
    void addressIsRequired() {
        var empty = new ChecklistContent.Address("1301", " ", null, null, null, "", "São Paulo");

        List<String> problems = ChecklistRules.problems(ChecklistFixtures.withAddress(ChecklistFixtures.valid(), empty));

        assertThat(problems).hasSize(6).allMatch(p -> p.startsWith("Etapa 2 — endereço principal"));
    }

    @Test
    @DisplayName("entrega em outro endereço exige o endereço de entrega")
    void deliveryAddressWhenDifferent() {
        var v = ChecklistFixtures.valid();
        var content = new ChecklistContent(v.customer(), v.mainAddress(), false, null, v.unitContact(),
                v.installation(), v.comodato(), v.visual(), v.order());

        assertThat(ChecklistRules.problems(content))
                .containsExactly("Etapa 2 — endereço de entrega: preencha o endereço.");
    }

    @Test
    @DisplayName("máquina: tipo da lista, 'Outra' pede o nome, e mesa é só Sim ou Não")
    void machineRules() {
        var v = ChecklistFixtures.valid();
        var installation = new ChecklistContent.Installation(false, true, List.of(
                new ChecklistContent.Machine("OUTRA", " ", 1, false),
                new ChecklistContent.Machine("LAVA-JATO", null, 0, true),
                new ChecklistContent.Machine("CAPO", null, 1, null)), null);
        var content = new ChecklistContent(v.customer(), v.mainAddress(), true, null, v.unitContact(),
                installation, v.comodato(), v.visual(), v.order());

        assertThat(ChecklistRules.problems(content)).containsExactly(
                "Etapa 4 — máquina 1: escreva qual é a máquina.",
                "Etapa 4 — máquina 2: escolha o tipo.",
                "Etapa 4 — máquina 2: a quantidade precisa ser pelo menos 1.",
                "Etapa 4 — máquina 3: responda se vai com mesa.");
    }

    @Test
    @DisplayName("quem assina: nome e sobrenome, e a inicial não conta como sobrenome")
    void signatoryFullName() {
        assertThat(ChecklistRules.fullName("Maria Souza")).isTrue();
        assertThat(ChecklistRules.fullName("  Maria   Aparecida de Souza ")).isTrue();
        assertThat(ChecklistRules.fullName("João D'Ávila")).isTrue();
        assertThat(ChecklistRules.fullName("Maria")).isFalse();
        assertThat(ChecklistRules.fullName("Maria S.")).isFalse();
        assertThat(ChecklistRules.fullName("Maria 123")).isFalse();

        var c = ChecklistFixtures.customer();
        var one = new ChecklistContent.Customer(c.code(), false, c.name(), c.legalName(), c.document(),
                c.stateRegistration(), c.mainPhone(), c.mobile(), "Maria", c.signatoryCpf(), c.invoiceEmail(),
                c.contractEmail(), c.priceTable(), c.erp());
        assertThat(ChecklistRules.problems(ChecklistFixtures.withCustomer(ChecklistFixtures.valid(), one)))
                .containsExactly("Etapa 3 — escreva o nome e o sobrenome de quem assina.");
    }

    @Test
    @DisplayName("pedido é opcional, mas quando existe precisa de produto, embalagem e preço")
    void orderRules() {
        var v = ChecklistFixtures.valid();
        var noOrder = new ChecklistContent(v.customer(), v.mainAddress(), true, null, v.unitContact(),
                v.installation(), v.comodato(), v.visual(), new ChecklistContent.Order(false, null, null, null));
        assertThat(ChecklistRules.problems(noOrder)).isEmpty();

        var emptyOrder = new ChecklistContent(v.customer(), v.mainAddress(), true, null, v.unitContact(),
                v.installation(), v.comodato(), v.visual(), new ChecklistContent.Order(true, "VENDA", List.of(), null));
        assertThat(ChecklistRules.problems(emptyOrder))
                .containsExactly("Etapa 7 — o pedido precisa de pelo menos um produto (ou marque \"sem pedido\").");

        var badItem = new ChecklistContent(v.customer(), v.mainAddress(), true, null, v.unitContact(),
                v.installation(), v.comodato(), v.visual(), new ChecklistContent.Order(true, "VENDA", List.of(
                new ChecklistContent.OrderItem(1, "X", "LT", null, null, 0, null, null, 80, "GERAL", null)), null));
        assertThat(ChecklistRules.problems(badItem)).hasSize(3);
    }

    @Test
    @DisplayName("o total do pedido é refeito no servidor: embalagens × tamanho × preço + IPI")
    void orderTotalIsRecomputed() {
        ChecklistContent recomputed = ChecklistFixtures.valid().withRecomputedOrder();

        // 3 × 20 LT × R$ 10,98 × 1,0325 = 680,21 ; 2 × 7,5 KG × R$ 35,31 = 529,65
        assertThat(recomputed.order().items()).extracting(ChecklistContent.OrderItem::lineTotal)
                .containsExactly(new BigDecimal("680.21"), new BigDecimal("529.65"));
        assertThat(recomputed.order().total()).isEqualByComparingTo("1209.86");
    }

    @Test
    @DisplayName("CPF e CNPJ: dígito verificador e sequência repetida")
    void documents() {
        assertThat(BrazilianDocument.isValidCnpj("11.222.333/0001-81")).isTrue();
        assertThat(BrazilianDocument.isValidCnpj("11.222.333/0001-80")).isFalse();
        assertThat(BrazilianDocument.isValidCpf("529.982.247-25")).isTrue();
        assertThat(BrazilianDocument.isValidCpf("529.982.247-24")).isFalse();
        assertThat(BrazilianDocument.isValidCpf("111.111.111-11")).isFalse();
        assertThat(BrazilianDocument.isValidCpfOrCnpj("52998224725")).isTrue();
        assertThat(BrazilianDocument.isValidCpfOrCnpj("123")).isFalse();
    }
}
