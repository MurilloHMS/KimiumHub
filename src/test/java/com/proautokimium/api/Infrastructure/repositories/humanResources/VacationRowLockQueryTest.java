package com.proautokimium.api.Infrastructure.repositories.humanResources;

import com.proautokimium.api.Infrastructure.repositories.EmployeeRepository;
import com.proautokimium.api.domain.entities.Employee;
import com.proautokimium.api.domain.entities.humanResources.VacationRequest;
import com.proautokimium.api.domain.valueObjects.Email;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.test.context.ActiveProfiles;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * **As leituras com trava rodam num banco de verdade.**
 *
 * O `FOR UPDATE` é SQL que o Hibernate monta por dialeto; um mock nunca o
 * executaria. Isto prova que a JPQL e a trava sobem no H2 e devolvem a linha.
 *
 * O que NÃO está provado aqui é a concorrência em si — duas transações
 * disputando a linha. Teste de duas threads contra banco é lento e instável;
 * a garantia vem do Postgres, que é quem honra o `FOR UPDATE` em produção.
 */
@DataJpaTest
@ActiveProfiles("test")
class VacationRowLockQueryTest {

    @Autowired TestEntityManager entityManager;
    @Autowired EmployeeRepository employeeRepository;
    @Autowired VacationRequestRepository vacationRequestRepository;

    private Employee funcionario() {
        Employee e = new Employee();
        e.setCodParceiro("2001");
        e.setAtivo(true);
        e.setEmail(new Email("lock@teste.com"));
        e.setVacationBalanceDays(20);
        return entityManager.persistAndFlush(e);
    }

    @Test
    @DisplayName("lê o funcionário travado, com o saldo gravado")
    void funcionarioTravado() {
        Employee e = funcionario();
        entityManager.clear();

        assertThat(employeeRepository.findByIdForUpdate(e.getId()))
                .get().extracting(Employee::getVacationBalanceDays).isEqualTo(20);
    }

    @Test
    @DisplayName("lê o pedido de férias travado")
    void pedidoTravado() {
        VacationRequest r = VacationRequest.request(funcionario(),
                LocalDate.of(2026, 10, 1), LocalDate.of(2026, 10, 10), null, LocalDateTime.of(2026, 9, 1, 9, 0));
        entityManager.persistAndFlush(r);
        entityManager.clear();

        assertThat(vacationRequestRepository.findByIdForUpdate(r.getId())).isPresent();
    }

    @Test
    @DisplayName("id inexistente devolve vazio, não erro")
    void inexistente() {
        assertThat(employeeRepository.findByIdForUpdate(UUID.randomUUID())).isEmpty();
        assertThat(vacationRequestRepository.findByIdForUpdate(UUID.randomUUID())).isEmpty();
    }
}
