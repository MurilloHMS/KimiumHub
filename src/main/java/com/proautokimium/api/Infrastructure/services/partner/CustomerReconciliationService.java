package com.proautokimium.api.Infrastructure.services.partner;

import com.proautokimium.api.Application.DTOs.partners.reconciliation.FieldDiffDTO;
import com.proautokimium.api.Application.DTOs.partners.reconciliation.ImpedimentDTO;
import com.proautokimium.api.Application.DTOs.partners.reconciliation.ReconciliationDTO;
import com.proautokimium.api.Application.DTOs.partners.reconciliation.ReconciliationRowDTO;
import com.proautokimium.api.Infrastructure.repositories.CustomerRepository;
import com.proautokimium.api.Infrastructure.repositories.EmployeeRepository;
import com.proautokimium.api.Infrastructure.utils.LinhaSankhya;
import com.proautokimium.api.Infrastructure.utils.RowSignature;
import com.proautokimium.api.domain.entities.Customer;
import com.proautokimium.api.domain.entities.Employee;
import com.proautokimium.api.domain.enums.Impediments;
import com.proautokimium.api.domain.valueObjects.Email;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Concilia o cadastro de clientes com o Sankhya.
 *
 * <p>Compara o que o ERP tem com o que existe aqui e devolve o que mudou, para
 * a pessoa decidir linha a linha. <b>Não grava nada:</b> a prévia é só leitura,
 * e é por isso que ela pode ser refeita à vontade.
 *
 * <p>Cada linha leva uma {@link RowSignature}. É ela que substitui uma tabela de
 * rascunho: no aplicar, o ERP é consultado de novo e a assinatura tem que bater
 * — senão a linha mudou desde que a pessoa olhou, e gravar seria escrever algo
 * que ela não aprovou.
 */
@Service
public class CustomerReconciliationService {

    /** Os campos que a conciliação compara e grava. Nada além disto é tocado. */
    static final String NAME = "nome";
    static final String DOCUMENT = "documento";
    static final String EMAIL = "email";
    static final String MATRIZ = "codigoMatriz";

    private final PartnerSankhyaQueryService sankhyaQueryService;
    private final CustomerRepository customerRepository;
    private final EmployeeRepository employeeRepository;

    public CustomerReconciliationService(PartnerSankhyaQueryService sankhyaQueryService, CustomerRepository customerRepository, EmployeeRepository employeeRepository) {
        this.sankhyaQueryService = sankhyaQueryService;
        this.customerRepository = customerRepository;
        this.employeeRepository = employeeRepository;
    }

    // ── A prévia ─────────────────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public ReconciliationDTO preview(LocalDate since) {
        LocalState local = loadLocalState();

        List<ReconciliationRowDTO> toCreate = new ArrayList<>();
        List<ReconciliationRowDTO> toUpdate = new ArrayList<>();
        List<ReconciliationRowDTO> toDeactivate = new ArrayList<>();
        int unchanged = 0;

        for (LinhaSankhya row : sankhyaQueryService.clientes(since)) {
            String code = text(row.inteiro("codigo"));
            Customer current = code == null ? null : local.byCode().get(code);

            List<FieldDiffDTO> differences = differencesBetween(current, row);
            List<ImpedimentDTO> impediments = impedimentsFor(row, code, current, local);

            ReconciliationRowDTO line = lineOf(row, code, differences, impediments);

            boolean activeInErp = "S".equalsIgnoreCase(row.texto("ativo"));

            if (current == null) {
                // Inativo no ERP e inexistente aqui não é decisão, é ruído: não
                // se cria um cliente já desativado.
                if (activeInErp) {
                    toCreate.add(line);
                }
                continue;
            }

            // A precedência é decisão dele: desativar é a ação mais consequente,
            // e a linha continua carregando as diferenças para o quadro ficar
            // visível. Sem isto escrito, qual balde vale dependeria da ordem dos
            // `if` e ninguém saberia qual.
            if (current.isAtivo() && !activeInErp) {
                toDeactivate.add(line);
            } else if (!differences.isEmpty()) {
                toUpdate.add(line);
            } else {
                unchanged++;
            }
        }

        return new ReconciliationDTO(toCreate, toUpdate, toDeactivate, unchanged);
    }

    // ── As diferenças ────────────────────────────────────────────────────────

    /**
     * Só os campos que mudaram, com os dois lados.
     *
     * <p>A tela desenha isso como <i>daqui → de lá</i>. Sem os dois valores, o
     * "o que muda" vira só "mudou", e a pessoa teria que abrir o cadastro de
     * cada um para decidir.
     *
     * <p>O {@code ativo} fica de fora de propósito: ele não é uma diferença de
     * campo, é o que decide o balde.
     */
    private List<FieldDiffDTO> differencesBetween(Customer current, LinhaSankhya row) {
        if (current == null) {
            return List.of();
        }

        List<FieldDiffDTO> differences = new ArrayList<>();

        compare(differences, NAME, current.getName(), row.texto("nome"));
        compare(differences, EMAIL, emailOf(current), row.texto("email"));
        compare(differences, MATRIZ, current.getCodigoMatriz(), text(row.inteiro("codigo_matriz")));

        // O documento compara por dígitos: "12.345.678/0001-99" e
        // "12345678000199" são o mesmo CNPJ, e sem normalizar toda linha
        // formatada apareceria como divergente.
        compare(differences, DOCUMENT, onlyDigits(current.getDocumento()), onlyDigits(row.texto("documento")));

        return differences;
    }

    /** Nulo e vazio são a mesma coisa: campo ausente dos dois lados não é diferença. */
    private void compare(List<FieldDiffDTO> into, String field, String local, String erp) {
        String a = blankToNull(local);
        String b = blankToNull(erp);

        if (!Objects.equals(a, b)) {
            into.add(new FieldDiffDTO(field, a, b));
        }
    }

    // ── Os impedimentos ──────────────────────────────────────────────────────

    /**
     * Tudo que impede a linha de ser aplicada.
     *
     * <p>Acumula, e não para no primeiro: quem for consertar no ERP quer saber
     * de tudo de uma vez, em vez de descobrir o segundo problema depois de
     * arrumar o primeiro.
     *
     * <p>Impedimento <b>trava a linha inteira</b>, mesmo quando ela é só uma
     * atualização. Aplicar metade é o tipo de coisa que ninguém lembra depois, e
     * a tela diria "atualizado" para algo meio atualizado.
     */
    private List<ImpedimentDTO> impedimentsFor(LinhaSankhya row, String code, Customer current, LocalState local) {
        List<ImpedimentDTO> impediments = new ArrayList<>();

        if (code == null) {
            impediments.add(new ImpedimentDTO(Impediments.NO_CODE, "O parceiro veio sem código no ERP."));
            return impediments;
        }

        String email = row.texto("email");
        if (!Email.isValid(email)) {
            impediments.add(new ImpedimentDTO(Impediments.EMAIL_INVALID,
                    email == null || email.isBlank()
                            ? "Sem e-mail no ERP."
                            : "E-mail inválido no ERP: " + email));
        }

        // Não pode acontecer para quem já é cliente — cod_parceiro é único em
        // `parceiros` desde a V101 —, mas pode para quem entraria agora.
        if (current == null && local.employeeCodes().contains(code)) {
            impediments.add(new ImpedimentDTO(Impediments.CODE_BELONGS_TO_EMPLOYEE,
                    "O código " + code + " já é de um funcionário."));
        }

        String digits = onlyDigits(row.texto("documento"));
        if (digits != null && !digits.isBlank()) {
            Customer owner = local.byCnpj().get(digits);

            // Se o dono é ele mesmo, não é conflito — é o próprio cadastro.
            if (owner != null && !Objects.equals(owner.getCodParceiro(), code)) {
                impediments.add(new ImpedimentDTO(Impediments.CNPJ_FROM_OTHER_CLIENT,
                        "O CNPJ já é do cliente " + owner.getCodParceiro() + " — " + owner.getName() + "."));
            }
        }

        return impediments;
    }

    // ── A linha ──────────────────────────────────────────────────────────────

    private ReconciliationRowDTO lineOf(LinhaSankhya row, String code,
                                        List<FieldDiffDTO> differences,
                                        List<ImpedimentDTO> impediments) {
        String name = row.texto("nome");
        String document = onlyDigits(row.texto("documento"));
        String email = row.texto("email");
        String matrizCode = text(row.inteiro("codigo_matriz"));
        boolean active = "S".equalsIgnoreCase(row.texto("ativo"));

        return new ReconciliationRowDTO(
                code, name, document, email, matrizCode, active,
                RowSignature.of(name, document, email, matrizCode, active ? "S" : "N"),
                differences, impediments);
    }

    // ── O estado local ───────────────────────────────────────────────────────

    /**
     * O que já existe aqui, carregado de uma vez.
     *
     * <p>São 2008 linhas vindas do ERP. Consultar o banco dentro do laço seriam
     * 2008 idas — e é exatamente o método que estoura quando há código
     * duplicado, porque {@code findByCodParceiro} devolve um só.
     */
    private record LocalState(
            Map<String, Customer> byCode,
            Map<String, Customer> byCnpj,
            Set<String> employeeCodes
    ) { }

    private LocalState loadLocalState() {
        // O discriminador da tabela já filtra perfil='CLIENTE' aqui.
        List<Customer> customers = customerRepository.findAll();

        Map<String, Customer> byCode = customers.stream()
                .filter(c -> c.getCodParceiro() != null && !c.getCodParceiro().isBlank())
                // Sem função de desempate de propósito: com código repetido isto
                // estoura, e é o que se quer. O índice único da V101 garante que
                // não acontece — se acontecer, alguma coisa furou a garantia e é
                // melhor saber agora do que conciliar contra dado inconsistente.
                .collect(Collectors.toMap(Customer::getCodParceiro, Function.identity()));

        // Chaveado pelos DÍGITOS: "12.345.678/0001-99" e "12345678000199" são o
        // mesmo CNPJ, e o índice ux_parceiros_cnpj_digits também é sobre eles.
        // O filtro vem antes porque toMap estoura com chave nula, e documento
        // vazio existe na base.
        Map<String, Customer> byCnpj = customers.stream()
                .filter(c -> c.getDocumento() != null && !c.getDocumento().isBlank())
                .collect(Collectors.toMap(
                        c -> onlyDigits(c.getDocumento()),
                        Function.identity(),
                        // Aqui o desempate existe: o índice de CNPJ é parcial
                        // (só a matriz), então dois clientes podem legitimamente
                        // dividir o documento. Ficar com um basta — o impedimento
                        // só precisa dizer QUE já é de outro.
                        (a, b) -> a));

        // Um código que já é de funcionário não pode virar cliente: cod_parceiro
        // é único em `parceiros` desde a V101, então o insert bateria no índice.
        // Melhor pegar na prévia, com o motivo na tela.
        Set<String> employeeCodes = employeeRepository.findAll().stream()
                .map(Employee::getCodParceiro)
                .filter(Objects::nonNull)
                .filter(code -> !code.isBlank())
                .collect(Collectors.toSet());

        return new LocalState(byCode, byCnpj, employeeCodes);
    }

    // ── Miudezas ─────────────────────────────────────────────────────────────

    private String emailOf(Customer customer) {
        return customer.getEmail() == null ? null : customer.getEmail().getAddress();
    }

    /**
     * Inteiro do ERP virando texto.
     *
     * <p><b>Zero vira nulo:</b> {@code CODPARCMATRIZ} vem 0 quando não há grupo,
     * e guardar "0" faria o cliente apontar para um parceiro que não existe.
     *
     * <p>E o caminho é {@code int}, nunca {@code double} — foi um
     * {@code String.valueOf(double)} que gravou "7.0" em 7149 linhas e deixou o
     * portal do cliente sem mostrar unidade nenhuma.
     */
    private String text(int value) {
        return value == 0 ? null : String.valueOf(value);
    }

    private static String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    private static String onlyDigits(String value) {
        return value == null ? null : value.replaceAll("[^0-9]", "");
    }
}
