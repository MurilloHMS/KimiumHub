package com.proautokimium.api.Infrastructure.services.partner;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.proautokimium.api.Application.DTOs.partners.reconciliation.FieldDiffDTO;
import com.proautokimium.api.Application.DTOs.partners.reconciliation.ImpedimentDTO;
import com.proautokimium.api.Application.DTOs.partners.reconciliation.ReconciliationDTO;
import com.proautokimium.api.Application.DTOs.partners.reconciliation.ReconciliationRowDTO;
import com.proautokimium.api.Infrastructure.repositories.CustomerRepository;
import com.proautokimium.api.Infrastructure.repositories.EmployeeRepository;
import com.proautokimium.api.Infrastructure.utils.LinhaSankhya;
import com.proautokimium.api.Infrastructure.utils.SankhyaRows;
import com.proautokimium.api.domain.entities.Customer;
import com.proautokimium.api.domain.entities.Employee;
import com.proautokimium.api.domain.enums.Impediments;
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
import static org.mockito.Mockito.when;

/**
 * A conciliação de clientes com o Sankhya.
 *
 * <p>Os dados vêm da medição de 2026-09-09: 2008 linhas do ERP contra ~7,4 mil
 * clientes locais. As armadilhas testadas aqui são todas reais — o CNPJ que se
 * repete porque uma empresa é várias unidades, o código que já é de um
 * funcionário, e o {@code CODPARCMATRIZ} que chega como inteiro e virava "7.0".
 *
 * <p><b>Nenhuma delas aparece na tela quando dá errado.</b> A conciliação
 * mostraria um número plausível, e o erro só seria descoberto no dia em que
 * alguém conferisse um cliente à mão.
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class CustomerReconciliationServiceTest {

    @Mock PartnerSankhyaQueryService sankhya;
    @Mock CustomerRepository customerRepository;
    @Mock EmployeeRepository employeeRepository;

    private CustomerReconciliationService service;
    private final ObjectMapper mapper = new ObjectMapper();

    private static final LocalDate DESDE = LocalDate.of(2025, 9, 9);

    @BeforeEach
    void setUp() {
        service = new CustomerReconciliationService(sankhya, customerRepository, employeeRepository);
        when(customerRepository.findAll()).thenReturn(List.of());
        when(employeeRepository.findAll()).thenReturn(List.of());
    }

    // ── Ajuda ────────────────────────────────────────────────────────────────

    /** Uma linha do ERP no formato real: colunas de um lado, valores do outro. */
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
        Customer c = new Customer(code, document, name, null, new Email(email), active, true, matriz, false);
        return c;
    }

    private void localHas(Customer... customers) {
        when(customerRepository.findAll()).thenReturn(List.of(customers));
    }

    // ── Os quatro baldes ─────────────────────────────────────────────────────

    @Test
    @DisplayName("parceiro que não existe aqui entra em toCreate")
    void newPartnerGoesToCreate() {
        erpReturns("8805,\"EXAL VESUVIUS\",\"exal@x.com\",\"03117442000188\",8805,\"S\"");

        ReconciliationDTO result = service.preview(DESDE);

        assertThat(result.toCreate()).hasSize(1);
        assertThat(result.toCreate().getFirst().code()).isEqualTo("8805");
        assertThat(result.toUpdate()).isEmpty();
        assertThat(result.unchanged()).isZero();
    }

    @Test
    @DisplayName("campo diferente entra em toUpdate, com os dois lados")
    void changedFieldGoesToUpdate() {
        localHas(customer("8781", "TEMPERO CERTO", "antigo@x.com", "111", "8781", true));
        erpReturns("8781,\"TEMPERO CERTO - PRESMAK\",\"novo@x.com\",\"111\",8781,\"S\"");

        ReconciliationDTO result = service.preview(DESDE);

        assertThat(result.toUpdate()).hasSize(1);
        assertThat(result.toUpdate().getFirst().differences())
                .extracting(FieldDiffDTO::field, FieldDiffDTO::localValue, FieldDiffDTO::erpValue)
                .contains(
                        org.assertj.core.groups.Tuple.tuple("nome", "TEMPERO CERTO", "TEMPERO CERTO - PRESMAK"),
                        org.assertj.core.groups.Tuple.tuple("email", "antigo@x.com", "novo@x.com"));
    }

    /**
     * <b>Sem esta linha, 1734 clientes iguais afogariam os 61 que mudaram.</b>
     * O número existe para a conta fechar, e nada mais.
     */
    @Test
    @DisplayName("tudo igual só conta, não vira lista")
    void unchangedIsOnlyCounted() {
        localHas(customer("8805", "EXAL", "exal@x.com", "03117442000188", "8805", true));
        erpReturns("8805,\"EXAL\",\"exal@x.com\",\"03117442000188\",8805,\"S\"");

        ReconciliationDTO result = service.preview(DESDE);

        assertThat(result.unchanged()).isEqualTo(1);
        assertThat(result.toCreate()).isEmpty();
        assertThat(result.toUpdate()).isEmpty();
        assertThat(result.toDeactivate()).isEmpty();
    }

    @Test
    @DisplayName("ativo aqui e inativo no ERP entra em toDeactivate")
    void inactiveInErpGoesToDeactivate() {
        localHas(customer("4", "UPBUS", "upbus@x.com", "20589268000118", "4", true));
        erpReturns("4,\"UPBUS\",\"upbus@x.com\",\"20589268000118\",4,\"N\"");

        ReconciliationDTO result = service.preview(DESDE);

        assertThat(result.toDeactivate()).hasSize(1);
        assertThat(result.toUpdate()).isEmpty();
    }

    /**
     * A precedência é decisão dele: desativar é a ação mais consequente, e a
     * linha continua carregando as diferenças para o quadro ficar visível.
     */
    @Test
    @DisplayName("inativo E divergente vai para toDeactivate, mostrando as diferenças")
    void inactiveWinsButKeepsDifferences() {
        localHas(customer("4", "UPBUS", "antigo@x.com", "205", "4", true));
        erpReturns("4,\"UPBUS QUALIDADE\",\"novo@x.com\",\"205\",4,\"N\"");

        ReconciliationDTO result = service.preview(DESDE);

        assertThat(result.toDeactivate()).hasSize(1);
        assertThat(result.toUpdate())
                .as("a mesma linha em dois baldes faria a contagem mentir")
                .isEmpty();
        assertThat(result.toDeactivate().getFirst().differences())
                .as("as diferenças continuam visíveis")
                .isNotEmpty();
    }

    /** Inativo no ERP e inexistente aqui não é decisão, é ruído. */
    @Test
    @DisplayName("inativo no ERP que não existe aqui não aparece em lugar nenhum")
    void inactiveAndUnknownIsIgnored() {
        erpReturns("545,\"COPLAC - NAO USAR\",\"coplac@x.com\",\"999\",545,\"N\"");

        ReconciliationDTO result = service.preview(DESDE);

        assertThat(result.toCreate()).isEmpty();
        assertThat(result.toUpdate()).isEmpty();
        assertThat(result.toDeactivate()).isEmpty();
        assertThat(result.unchanged()).isZero();
    }

    // ── Os impedimentos ──────────────────────────────────────────────────────

    @Test
    @DisplayName("e-mail inválido no ERP vira impedimento, com o texto original")
    void invalidEmailIsAnImpediment() {
        erpReturns("7712,\"METALCORP\",\"compras@\",\"44190077000105\",7712,\"S\"");

        ReconciliationRowDTO line = service.preview(DESDE).toCreate().getFirst();

        assertThat(line.impediments()).extracting(ImpedimentDTO::reason)
                .containsExactly(Impediments.EMAIL_INVALID);
        assertThat(line.impediments().getFirst().detail())
                .as("o valor original é o que permite ir consertar no ERP")
                .contains("compras@");
    }

    /**
     * <b>O impedimento que só existe depois da V101.</b> Antes, um código que já
     * era de funcionário passava e criava dois parceiros com o mesmo CODPARC.
     * Agora bateria no índice único — melhor pegar na prévia.
     */
    @Test
    @DisplayName("código que já é de funcionário vira impedimento")
    void employeeCodeIsAnImpediment() {
        Employee employee = new Employee();
        employee.setCodParceiro("3418");
        when(employeeRepository.findAll()).thenReturn(List.of(employee));
        erpReturns("3418,\"JOSE CARLOS\",\"jose@x.com\",\"82111440830\",3418,\"S\"");

        ReconciliationRowDTO line = service.preview(DESDE).toCreate().getFirst();

        assertThat(line.impediments()).extracting(ImpedimentDTO::reason)
                .contains(Impediments.CODE_BELONGS_TO_EMPLOYEE);
        assertThat(line.impediments().getFirst().detail()).contains("3418");
    }

    @Test
    @DisplayName("CNPJ que já é de outro cliente vira impedimento, nomeando o dono")
    void cnpjOfAnotherClientIsAnImpediment() {
        localHas(customer("1708", "PGR MATRIZ", "pgr@x.com", "17776957000127", "1708", true));
        erpReturns("505,\"PGR - SINTER\",\"sinter@x.com\",\"17776957000127\",1708,\"S\"");

        ReconciliationRowDTO line = service.preview(DESDE).toCreate().getFirst();

        assertThat(line.impediments()).extracting(ImpedimentDTO::reason)
                .contains(Impediments.CNPJ_FROM_OTHER_CLIENT);
        assertThat(line.impediments().getFirst().detail())
                .as("quem for resolver precisa saber de quem é")
                .contains("1708").contains("PGR MATRIZ");
    }

    /**
     * O próprio cadastro não é conflito consigo mesmo. Sem esta checagem, os
     * ~7,4 mil clientes existentes apareceriam todos impedidos.
     */
    @Test
    @DisplayName("o CNPJ do próprio cliente não é impedimento")
    void ownCnpjIsNotAnImpediment() {
        localHas(customer("8781", "TEMPERO", "a@x.com", "17776957000127", "8781", true));
        erpReturns("8781,\"TEMPERO NOVO\",\"a@x.com\",\"17776957000127\",8781,\"S\"");

        ReconciliationRowDTO line = service.preview(DESDE).toUpdate().getFirst();

        assertThat(line.impediments()).isEmpty();
    }

    /** Acumula, e não para no primeiro: quem conserta no ERP quer ver tudo. */
    @Test
    @DisplayName("dois problemas na mesma linha viram dois impedimentos")
    void impedimentsAccumulate() {
        Employee employee = new Employee();
        employee.setCodParceiro("3418");
        when(employeeRepository.findAll()).thenReturn(List.of(employee));
        erpReturns("3418,\"JOSE CARLOS\",\"jose@\",\"82111440830\",3418,\"S\"");

        ReconciliationRowDTO line = service.preview(DESDE).toCreate().getFirst();

        assertThat(line.impediments()).extracting(ImpedimentDTO::reason)
                .containsExactlyInAnyOrder(Impediments.EMAIL_INVALID, Impediments.CODE_BELONGS_TO_EMPLOYEE);
    }

    // ── As armadilhas de conversão ───────────────────────────────────────────

    /**
     * <b>O defeito que custou o portal do cliente.</b> O {@code CODPARCMATRIZ}
     * chega como inteiro; um caminho por {@code double} grava "1708.0", e aí
     * {@code findByCodigoMatriz} nunca casa. Aqui o sintoma seria outro: toda
     * linha vira divergente.
     */
    @Test
    @DisplayName("codigo_matriz inteiro não vira 1708.0 nem divergência falsa")
    void matrizCodeDoesNotBecomeDouble() {
        localHas(customer("505", "PGR", "pgr@x.com", "111", "1708", true));
        erpReturns("505,\"PGR\",\"pgr@x.com\",\"111\",1708,\"S\"");

        ReconciliationDTO result = service.preview(DESDE);

        assertThat(result.unchanged())
                .as("1708 do ERP e \"1708\" daqui são o mesmo grupo")
                .isEqualTo(1);
    }

    /** Sem grupo, o ERP manda 0 — e "0" apontaria para um parceiro que não existe. */
    @Test
    @DisplayName("codigo_matriz zero vira ausente, não o texto zero")
    void zeroMatrizBecomesAbsent() {
        erpReturns("900,\"SEM GRUPO\",\"x@x.com\",\"111\",0,\"S\"");

        ReconciliationRowDTO line = service.preview(DESDE).toCreate().getFirst();

        assertThat(line.matrizCode()).isNull();
    }

    /**
     * As colunas do TGFPAR são CHAR e vêm com espaço à direita. O documento
     * compara por dígitos pelo mesmo motivo: formatado de um lado e cru do
     * outro é o mesmo CNPJ.
     */
    @Test
    @DisplayName("documento formatado e cru são o mesmo, e não divergência")
    void documentComparesByDigits() {
        localHas(customer("8805", "EXAL", "exal@x.com", "03.117.442/0001-88", "8805", true));
        erpReturns("8805,\"EXAL\",\"exal@x.com\",\"03117442000188\",8805,\"S\"");

        assertThat(service.preview(DESDE).unchanged()).isEqualTo(1);
    }

    // ── A assinatura ─────────────────────────────────────────────────────────

    /** É ela que substitui a tabela de rascunho. Linha sem assinatura não aplica. */
    @Test
    @DisplayName("toda linha leva assinatura")
    void everyLineCarriesASignature() {
        erpReturns("8805,\"EXAL\",\"exal@x.com\",\"111\",8805,\"S\"");

        assertThat(service.preview(DESDE).toCreate().getFirst().signature())
                .isNotBlank()
                .hasSize(12);
    }
}
