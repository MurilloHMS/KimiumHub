package com.proautokimium.api.Infrastructure.services.partner;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.proautokimium.api.Application.DTOs.partners.ErpPartnerDTO;
import com.proautokimium.api.Infrastructure.repositories.CustomerRepository;
import com.proautokimium.api.Infrastructure.repositories.EmployeeRepository;
import com.proautokimium.api.Infrastructure.utils.LinhaSankhya;
import com.proautokimium.api.Infrastructure.utils.SankhyaRows;
import com.proautokimium.api.domain.entities.Customer;
import com.proautokimium.api.domain.entities.Employee;
import com.proautokimium.api.domain.enums.PartnerConflict;
import com.proautokimium.api.domain.exceptions.partners.ErpPartnerNotFoundException;
import com.proautokimium.api.domain.valueObjects.Email;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

/**
 * Buscar um parceiro do Sankhya para preencher o cadastro de funcionário.
 *
 * <p>Antes disto, cadastrar quem já existe no ERP era redigitar nome, CPF e
 * e-mail. Um dígito errado no CPF não aparece em lugar nenhum — só no primeiro
 * acesso, que é por CPF e simplesmente não encontra a pessoa.
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class ErpPartnerLookupServiceTest {

    @Mock PartnerSankhyaQueryService sankhya;
    @Mock CustomerRepository customerRepository;
    @Mock EmployeeRepository employeeRepository;

    private ErpPartnerLookupService service;
    private final ObjectMapper mapper = new ObjectMapper();

    @BeforeEach
    void setUp() {
        service = new ErpPartnerLookupService(sankhya, customerRepository, employeeRepository);
        when(customerRepository.findByCodParceiro(anyString())).thenReturn(Optional.empty());
        when(employeeRepository.findByCodParceiro(anyString())).thenReturn(null);
    }

    private void erpReturns(String... values) {
        String json = "{\"fieldsMetadata\":["
                + "{\"name\":\"codigo\"},{\"name\":\"nome\"},{\"name\":\"email\"},"
                + "{\"name\":\"documento\"},{\"name\":\"ativo\"},{\"name\":\"cliente\"},{\"name\":\"tipo_pessoa\"}"
                + "],\"rows\":[" + (values.length == 0 ? "" : "[" + String.join(",", values) + "]") + "]}";

        List<LinhaSankhya> rows = SankhyaRows.emLinhas(json, mapper);
        when(sankhya.parceiroPorCodigo(anyInt())).thenReturn(rows);
    }

    // ── O caminho normal ─────────────────────────────────────────────────────

    @Test
    @DisplayName("traz nome, documento e e-mail do parceiro")
    void bringsTheFields() {
        erpReturns("3418", "\"JOSE CARLOS ALVES\"", "\"jose@empresa.com.br\"", "\"82111440830\"", "\"S\"", "\"N\"", "\"F\"");

        ErpPartnerDTO result = service.byCode(3418);

        assertThat(result.codParceiro()).isEqualTo("3418");
        assertThat(result.name()).isEqualTo("JOSE CARLOS ALVES");
        assertThat(result.document()).isEqualTo("82111440830");
        assertThat(result.email()).isEqualTo("jose@empresa.com.br");
        assertThat(result.activeInErp()).isTrue();
        assertThat(result.conflict()).isNull();
    }

    /** O documento vai só com dígitos: `parceiros.documento` é VARCHAR(14). */
    @Test
    @DisplayName("o documento vai sem pontuação")
    void documentWithoutPunctuation() {
        erpReturns("1", "\"ACME\"", "\"a@x.com\"", "\"821.114.408-30\"", "\"S\"", "\"N\"", "\"F\"");

        assertThat(service.byCode(1).document()).isEqualTo("82111440830");
    }

    /**
     * <b>E-mail que o value object recusaria não é devolvido.</b>
     *
     * <p>Preenchido na tela, a pessoa salvaria e o erro apareceria como 400 no
     * fim do cadastro. Vazio, ela digita — que é o que já faz hoje.
     */
    @Test
    @DisplayName("e-mail inválido no ERP vem vazio, e não preenche o campo")
    void invalidEmailComesEmpty() {
        erpReturns("1", "\"ACME\"", "\"compras@\"", "\"123\"", "\"S\"", "\"N\"", "\"J\"");

        assertThat(service.byCode(1).email()).isNull();
    }

    /**
     * Sem e-mail é legítimo: muitos funcionários não têm no ERP, e é por isso
     * que o primeiro acesso é por CPF.
     */
    @Test
    @DisplayName("parceiro sem e-mail não é erro")
    void missingEmailIsFine() {
        erpReturns("1", "\"ACME\"", "\"\"", "\"123\"", "\"S\"", "\"N\"", "\"J\"");

        ErpPartnerDTO result = service.byCode(1);

        assertThat(result.email()).isNull();
        assertThat(result.name()).isEqualTo("ACME");
    }

    // ── Os conflitos ─────────────────────────────────────────────────────────

    /**
     * <b>Desde a V101 o `cod_parceiro` é único.</b> Um código já usado não pode
     * virar funcionário — o insert bateria no índice. Dizer agora evita a pessoa
     * preencher empresa, setor, cargo, nível, contrato e data de admissão para
     * levar erro no fim.
     */
    @Test
    @DisplayName("código que já é de um funcionário volta como conflito, com o nome")
    void alreadyAnEmployee() {
        Employee existing = new Employee();
        existing.setCodParceiro("3418");
        existing.setName("JOSE CARLOS");
        when(employeeRepository.findByCodParceiro("3418")).thenReturn(existing);
        erpReturns("3418", "\"JOSE CARLOS\"", "\"a@x.com\"", "\"123\"", "\"S\"", "\"N\"", "\"F\"");

        ErpPartnerDTO result = service.byCode(3418);

        assertThat(result.conflict()).isEqualTo(PartnerConflict.ALREADY_AN_EMPLOYEE);
        assertThat(result.conflictWith())
                .as("quem vê o aviso precisa saber de quem é o código")
                .isEqualTo("JOSE CARLOS");
    }

    @Test
    @DisplayName("código que já é de um cliente volta como conflito")
    void alreadyACustomer() {
        Customer existing = new Customer("8805", "123", "EXAL VESUVIUS", null,
                new Email("exal@x.com"), true, true, "8805", false);
        when(customerRepository.findByCodParceiro("8805")).thenReturn(Optional.of(existing));
        erpReturns("8805", "\"EXAL VESUVIUS\"", "\"exal@x.com\"", "\"123\"", "\"S\"", "\"N\"", "\"J\"");

        ErpPartnerDTO result = service.byCode(8805);

        assertThat(result.conflict()).isEqualTo(PartnerConflict.ALREADY_A_CUSTOMER);
        assertThat(result.conflictWith()).isEqualTo("EXAL VESUVIUS");
    }

    /** Código livre não inventa conflito — seria um aviso que trava sem motivo. */
    @Test
    @DisplayName("código livre volta sem conflito")
    void freeCode() {
        erpReturns("999", "\"NOVO\"", "\"novo@x.com\"", "\"123\"", "\"S\"", "\"N\"", "\"F\"");

        assertThat(service.byCode(999).conflict()).isNull();
        assertThat(service.byCode(999).conflictWith()).isNull();
    }

    // ── Não existe ───────────────────────────────────────────────────────────

    /**
     * 404 e não lista vazia: quem digitou um código errado precisa de uma
     * resposta que diga isso, e não de um formulário que continua em branco sem
     * explicação.
     */
    @Test
    @DisplayName("código inexistente no ERP dá 404, dizendo qual foi")
    void notInErp() {
        erpReturns();

        assertThatThrownBy(() -> service.byCode(4242))
                .isInstanceOf(ErpPartnerNotFoundException.class)
                .hasMessageContaining("4242");
    }
}
