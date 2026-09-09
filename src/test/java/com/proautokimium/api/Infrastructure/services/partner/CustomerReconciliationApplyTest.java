package com.proautokimium.api.Infrastructure.services.partner;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.proautokimium.api.Application.DTOs.partners.reconciliation.ReconciliationApplyDTO;
import com.proautokimium.api.Application.DTOs.partners.reconciliation.ReconciliationChoiceDTO;
import com.proautokimium.api.Application.DTOs.partners.reconciliation.ReconciliationDTO;
import com.proautokimium.api.Application.DTOs.partners.reconciliation.ReconciliationResultDTO;
import com.proautokimium.api.Application.DTOs.partners.reconciliation.ReconciliationRowDTO;
import com.proautokimium.api.Infrastructure.repositories.CustomerRepository;
import com.proautokimium.api.Infrastructure.repositories.EmployeeRepository;
import com.proautokimium.api.Infrastructure.utils.LinhaSankhya;
import com.proautokimium.api.Infrastructure.utils.SankhyaRows;
import com.proautokimium.api.domain.entities.Customer;
import com.proautokimium.api.domain.enums.ReconciliationOutcome;
import com.proautokimium.api.domain.valueObjects.Email;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * O aplicar da conciliação.
 *
 * <p><b>É aqui que a decisão de não guardar prévia em tabela se paga ou se
 * perde.</b> Sem a assinatura, a aba aberta antes do almoço gravaria dado velho
 * sem nada avisar; sem a transação por linha, um e-mail ruim derrubaria as
 * outras 46 — que é exatamente o que o importador de Excel faz hoje.
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class CustomerReconciliationApplyTest {

    @Mock PartnerSankhyaQueryService sankhya;
    @Mock CustomerRepository customerRepository;
    @Mock EmployeeRepository employeeRepository;
    @Mock CustomerReconciliationWriter writer;

    private CustomerReconciliationService service;
    private final ObjectMapper mapper = new ObjectMapper();
    private static final LocalDate SINCE = LocalDate.of(2025, 9, 9);

    @BeforeEach
    void setUp() {
        service = new CustomerReconciliationService(sankhya, customerRepository, employeeRepository, writer);
        when(customerRepository.findAll()).thenReturn(List.of());
        when(employeeRepository.findAll()).thenReturn(List.of());
        when(writer.create(any())).thenReturn(ReconciliationOutcome.CREATED);
        when(writer.update(any(), any())).thenReturn(ReconciliationOutcome.UPDATED);
        when(writer.deactivate(any())).thenReturn(ReconciliationOutcome.DEACTIVATED);
    }

    private void erpReturns(String... rows) {
        StringBuilder json = new StringBuilder("{\"fieldsMetadata\":[")
                .append("{\"name\":\"codigo\"},{\"name\":\"nome\"},{\"name\":\"email\"},")
                .append("{\"name\":\"documento\"},{\"name\":\"codigo_matriz\"},{\"name\":\"ativo\"}")
                .append("],\"rows\":[");
        for (int i = 0; i < rows.length; i++) {
            json.append(i > 0 ? "," : "").append("[").append(rows[i]).append("]");
        }
        json.append("]}");
        List<LinhaSankhya> lines = SankhyaRows.emLinhas(json.toString(), mapper);
        when(sankhya.clientes(any(LocalDate.class))).thenReturn(lines);
    }

    private Customer customer(String code, String name, String email, String document, String matriz, boolean active) {
        return new Customer(code, document, name, null, new Email(email), active, true, matriz, false);
    }

    /** A assinatura que a prévia daria para aquela linha, agora. */
    private String signatureOf(String code) {
        ReconciliationDTO preview = service.preview(SINCE);
        return java.util.stream.Stream.of(preview.toCreate(), preview.toUpdate(), preview.toDeactivate())
                .flatMap(List::stream)
                .filter(r -> r.code().equals(code))
                .map(ReconciliationRowDTO::signature)
                .findFirst().orElseThrow();
    }

    private ReconciliationApplyDTO choose(String code, String signature) {
        return new ReconciliationApplyDTO(List.of(new ReconciliationChoiceDTO(code, signature)));
    }

    // ── O que grava ──────────────────────────────────────────────────────────

    @Test
    @DisplayName("cria o cliente escolhido")
    void createsTheChosenOne() {
        erpReturns("8805,\"EXAL\",\"exal@x.com\",\"03117442000188\",8805,\"S\"");

        ReconciliationResultDTO result = service.apply(SINCE, choose("8805", signatureOf("8805")));

        assertThat(result.created()).isEqualTo(1);
        verify(writer).create(any());
    }

    @Test
    @DisplayName("desativa quem ficou inativo no ERP")
    void deactivatesTheInactive() {
        when(customerRepository.findAll()).thenReturn(List.of(
                customer("4", "UPBUS", "upbus@x.com", "205", "4", true)));
        erpReturns("4,\"UPBUS\",\"upbus@x.com\",\"205\",4,\"N\"");

        ReconciliationResultDTO result = service.apply(SINCE, choose("4", signatureOf("4")));

        assertThat(result.deactivated()).isEqualTo(1);
        verify(writer).deactivate(any());
    }

    /**
     * <b>Só o que foi marcado.</b> A prévia traz três; a escolha traz uma.
     * Gravar as outras duas seria escrever o que ninguém aprovou.
     */
    @Test
    @DisplayName("grava só os códigos marcados")
    void writesOnlyWhatWasChosen() {
        erpReturns(
                "1,\"UM\",\"um@x.com\",\"111\",1,\"S\"",
                "2,\"DOIS\",\"dois@x.com\",\"222\",2,\"S\"",
                "3,\"TRES\",\"tres@x.com\",\"333\",3,\"S\"");

        service.apply(SINCE, choose("2", signatureOf("2")));

        verify(writer, org.mockito.Mockito.times(1)).create(any());
    }

    // ── O que recusa ─────────────────────────────────────────────────────────

    /**
     * <b>A razão de não haver tabela de rascunho.</b> A pessoa abriu a prévia,
     * o ERP mudou, e ela aplicou. A assinatura não bate e nada é gravado — ela
     * aprovou aquela mudança, não a que veio depois.
     */
    @Test
    @DisplayName("assinatura velha não grava")
    void staleSignatureIsRefused() {
        erpReturns("8805,\"EXAL\",\"exal@x.com\",\"111\",8805,\"S\"");

        ReconciliationResultDTO result = service.apply(SINCE, choose("8805", "assinaturaVelha"));

        assertThat(result.created()).isZero();
        assertThat(result.skipped()).isEqualTo(1);
        assertThat(result.lines()).extracting(l -> l.outcome())
                .containsExactly(ReconciliationOutcome.SKIPPED_CHANGED_IN_ERP);
        verify(writer, never()).create(any());
    }

    @Test
    @DisplayName("código que sumiu do ERP não grava")
    void goneFromErpIsRefused() {
        erpReturns("8805,\"EXAL\",\"exal@x.com\",\"111\",8805,\"S\"");

        ReconciliationResultDTO result = service.apply(SINCE, choose("9999", "qualquer"));

        assertThat(result.skipped()).isEqualTo(1);
        assertThat(result.lines()).extracting(l -> l.outcome())
                .containsExactly(ReconciliationOutcome.SKIPPED_GONE_FROM_ERP);
        verify(writer, never()).create(any());
    }

    /** Impedimento trava a linha inteira — decisão dele. */
    @Test
    @DisplayName("linha com impedimento não grava, mesmo escolhida")
    void impedimentBlocksTheLine() {
        erpReturns("7712,\"METALCORP\",\"compras@\",\"441\",7712,\"S\"");

        ReconciliationResultDTO result = service.apply(SINCE, choose("7712", signatureOf("7712")));

        assertThat(result.created()).isZero();
        assertThat(result.lines()).extracting(l -> l.outcome())
                .containsExactly(ReconciliationOutcome.REFUSED_IMPEDIMENT);
        assertThat(result.lines().getFirst().detail()).contains("compras@");
        verify(writer, never()).create(any());
    }

    /**
     * <b>Uma linha que falha não derruba as outras.</b> É a diferença para o
     * importador de Excel, que é transacional sobre o lote e devolve 500 sem
     * dizer qual linha.
     */
    @Test
    @DisplayName("uma linha que estoura não impede as outras")
    void oneFailureDoesNotStopTheRest() {
        erpReturns(
                "1,\"UM\",\"um@x.com\",\"111\",1,\"S\"",
                "2,\"DOIS\",\"dois@x.com\",\"222\",2,\"S\"",
                "3,\"TRES\",\"tres@x.com\",\"333\",3,\"S\"");

        when(writer.create(any())).thenAnswer(call -> {
            ReconciliationRowDTO row = call.getArgument(0);
            if (row.code().equals("2")) throw new IllegalStateException("banco recusou");
            return ReconciliationOutcome.CREATED;
        });

        ReconciliationResultDTO result = service.apply(SINCE, new ReconciliationApplyDTO(List.of(
                new ReconciliationChoiceDTO("1", signatureOf("1")),
                new ReconciliationChoiceDTO("2", signatureOf("2")),
                new ReconciliationChoiceDTO("3", signatureOf("3")))));

        assertThat(result.created()).isEqualTo(2);
        assertThat(result.skipped()).isEqualTo(1);
        assertThat(result.lines()).extracting(l -> l.outcome())
                .containsExactly(ReconciliationOutcome.REFUSED_ERROR);
        assertThat(result.lines().getFirst().detail()).contains("banco recusou");
    }

    /**
     * O resultado lista <b>só o que não deu no esperado</b>. As 47 que
     * funcionaram afogariam as 3 que não — e são essas que alguém precisa ver.
     */
    @Test
    @DisplayName("o que deu certo não vira linha no resultado")
    void successesAreNotListed() {
        erpReturns("8805,\"EXAL\",\"exal@x.com\",\"111\",8805,\"S\"");

        ReconciliationResultDTO result = service.apply(SINCE, choose("8805", signatureOf("8805")));

        assertThat(result.created()).isEqualTo(1);
        assertThat(result.lines()).isEmpty();
    }
}
