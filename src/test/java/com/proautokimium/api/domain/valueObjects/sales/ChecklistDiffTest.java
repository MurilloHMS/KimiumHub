package com.proautokimium.api.domain.valueObjects.sales;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/** O histórico do reenvio: o que mudou, com nome de gente. */
class ChecklistDiffTest {

    final ObjectMapper mapper = new ObjectMapper().registerModule(new JavaTimeModule());

    @Test
    @DisplayName("sem mudança, histórico vazio")
    void noChanges() {
        var c = ChecklistFixtures.valid();
        assertThat(ChecklistDiff.between(c, c, mapper)).isEmpty();
    }

    @Test
    @DisplayName("um campo do contrato mudou: o caminho é legível")
    void readablePath() {
        var before = ChecklistFixtures.valid();
        var c = before.customer();
        var customer = new ChecklistContent.Customer(c.code(), false, c.name(), c.legalName(), c.document(),
                c.stateRegistration(), c.mainPhone(), c.mobile(), c.signatory(), c.signatoryCpf(),
                "notas@mercado.com.br", c.contractEmail(), c.priceTable(), c.erp());

        var changes = ChecklistDiff.between(before, ChecklistFixtures.withCustomer(before, customer), mapper);

        assertThat(changes).containsExactly(new ChecklistDiff.Change(
                "Cliente e contrato › E-mail das notas fiscais", "financeiro@mercado.com.br", "notas@mercado.com.br"));
    }

    @Test
    @DisplayName("item de lista é identificado pelo nome, e item novo é uma linha só")
    void listItemsByName() {
        var before = ChecklistFixtures.valid();
        var comodato = new ChecklistContent.Comodato(List.of(
                new ChecklistContent.ComodatoItem(4250, "DOSADOR DHD 01", "DHD Proauto", 1),
                new ChecklistContent.ComodatoItem(1998, "DILUIDOR NTI - AZUL", "Diluidor padrão", 4)),
                List.of(), null);

        var changes = ChecklistDiff.between(before, ChecklistFixtures.withComodato(before, comodato), mapper);

        // O item antigo mudou de posição (era o 1º, virou o 2º) e não aparece como "mudou tudo".
        assertThat(changes).containsExactlyInAnyOrder(
                new ChecklistDiff.Change("Comodato › Itens › Diluidor padrão › Quantidade", "2", "4"),
                new ChecklistDiff.Change("Comodato › Itens › DHD Proauto › Quantidade", "", "1"));
    }

    @Test
    @DisplayName("o retrato do Sankhya e os totais por linha não entram no histórico")
    void skipsErpSnapshot() {
        var before = ChecklistFixtures.valid();
        var c = before.customer();
        var customer = new ChecklistContent.Customer(c.code(), false, c.name(), c.legalName(), c.document(),
                c.stateRegistration(), c.mainPhone(), c.mobile(), c.signatory(), c.signatoryCpf(),
                c.invoiceEmail(), c.contractEmail(), c.priceTable(), java.util.Map.of("name", "OUTRO"));

        var changes = ChecklistDiff.between(before.withRecomputedOrder(),
                ChecklistFixtures.withCustomer(before, customer), mapper);

        assertThat(changes).extracting(ChecklistDiff.Change::field)
                .noneMatch(f -> f.contains("erp") || f.contains("lineTotal"))
                // o total do pedido é informação para quem lê: ele sim aparece
                .contains("Pedido › Total");
    }

    @Test
    @DisplayName("valores de lista fechada saem em português")
    void translatedValues() {
        var before = ChecklistFixtures.valid();
        var i = before.installation();
        var installation = new ChecklistContent.Installation(i.withMaintenance(), true,
                List.of(new ChecklistContent.Machine("FRONTAL", null, 1, false)), null);
        var after = new ChecklistContent(before.customer(), before.mainAddress(), true, null, before.unitContact(),
                installation, before.comodato(), before.visual(), before.order());

        assertThat(ChecklistDiff.between(before, after, mapper)).containsExactly(
                new ChecklistDiff.Change("Instalação › Máquina › 1 › Tipo", "Capô", "Frontal"),
                new ChecklistDiff.Change("Instalação › Máquina › 1 › Vai com mesa", "Sim", "Não"));
    }
}
