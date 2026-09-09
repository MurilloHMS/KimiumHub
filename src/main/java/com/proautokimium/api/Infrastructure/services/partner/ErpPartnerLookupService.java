package com.proautokimium.api.Infrastructure.services.partner;

import com.proautokimium.api.Application.DTOs.partners.ErpPartnerDTO;
import com.proautokimium.api.Infrastructure.repositories.CustomerRepository;
import com.proautokimium.api.Infrastructure.repositories.EmployeeRepository;
import com.proautokimium.api.Infrastructure.utils.LinhaSankhya;
import com.proautokimium.api.domain.entities.Customer;
import com.proautokimium.api.domain.entities.Employee;
import com.proautokimium.api.domain.enums.PartnerConflict;
import com.proautokimium.api.domain.exceptions.partners.ErpPartnerNotFoundException;
import com.proautokimium.api.domain.valueObjects.Email;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Busca um parceiro no Sankhya para preencher o cadastro de funcionário.
 *
 * <p>Antes disto, cadastrar um funcionário que já existe no ERP era redigitar
 * nome, CPF e e-mail — e um dígito errado no CPF só aparece no primeiro acesso,
 * que é por CPF e simplesmente não encontra a pessoa.
 */
@Service
public class ErpPartnerLookupService {

    private final PartnerSankhyaQueryService sankhya;
    private final CustomerRepository customerRepository;
    private final EmployeeRepository employeeRepository;

    public ErpPartnerLookupService(PartnerSankhyaQueryService sankhya,
                                   CustomerRepository customerRepository,
                                   EmployeeRepository employeeRepository) {
        this.sankhya = sankhya;
        this.customerRepository = customerRepository;
        this.employeeRepository = employeeRepository;
    }

    @Transactional(readOnly = true)
    public ErpPartnerDTO byCode(int codParceiro) {
        List<LinhaSankhya> rows = sankhya.parceiroPorCodigo(codParceiro);

        if (rows.isEmpty()) {
            throw new ErpPartnerNotFoundException(codParceiro);
        }

        LinhaSankhya row = rows.getFirst();
        String code = String.valueOf(row.inteiro("codigo"));

        String email = row.texto("email");
        // E-mail que o value object recusaria não é devolvido: a tela o
        // preencheria, a pessoa salvaria, e o erro apareceria como 400 no fim.
        // Vazio, ela digita — que é o que já faz hoje.
        String usableEmail = Email.isValid(email) ? email : null;

        return new ErpPartnerDTO(
                code,
                row.texto("nome"),
                onlyDigits(row.texto("documento")),
                usableEmail,
                "S".equalsIgnoreCase(row.texto("ativo")),
                conflictOf(code),
                conflictName(code));
    }

    /**
     * O código já é de alguém aqui?
     *
     * <p>Desde a V101 o `cod_parceiro` é único em `parceiros`: um código já
     * usado não pode virar funcionário, e o insert bateria no índice. Dizer
     * agora evita a pessoa preencher empresa, setor, cargo, nível, contrato e
     * data de admissão para levar erro no fim.
     */
    private PartnerConflict conflictOf(String code) {
        if (employeeRepository.findByCodParceiro(code) != null) {
            return PartnerConflict.ALREADY_AN_EMPLOYEE;
        }
        if (customerRepository.findByCodParceiro(code).isPresent()) {
            return PartnerConflict.ALREADY_A_CUSTOMER;
        }
        return null;
    }

    private String conflictName(String code) {
        Employee employee = employeeRepository.findByCodParceiro(code);
        if (employee != null) {
            return employee.getName();
        }

        return customerRepository.findByCodParceiro(code)
                .map(Customer::getName)
                .orElse(null);
    }

    private static String onlyDigits(String value) {
        return value == null ? null : value.replaceAll("[^0-9]", "");
    }
}
