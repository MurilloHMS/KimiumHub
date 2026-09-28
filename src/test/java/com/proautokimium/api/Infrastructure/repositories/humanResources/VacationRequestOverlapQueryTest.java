package com.proautokimium.api.Infrastructure.repositories.humanResources;

import com.proautokimium.api.domain.entities.Employee;
import com.proautokimium.api.domain.entities.humanResources.VacationRequest;
import com.proautokimium.api.domain.enums.humanResources.VacationRequestStatus;
import com.proautokimium.api.domain.valueObjects.Email;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.test.context.ActiveProfiles;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static com.proautokimium.api.domain.enums.humanResources.VacationRequestStatus.APPROVED;
import static com.proautokimium.api.domain.enums.humanResources.VacationRequestStatus.PENDING;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * **A consulta de sobreposição do próprio funcionário, contra um banco de verdade.**
 *
 * O defeito original morava numa consulta (`vr.employee <> :employee`), e um
 * mock de repositório nunca teria visto — ele devolve o que o teste manda.
 * Aqui a JPQL roda no H2.
 */
@DataJpaTest
@ActiveProfiles("test")
class VacationRequestOverlapQueryTest {

    private static final List<VacationRequestStatus> ABERTAS = List.of(PENDING, APPROVED);
    private static final LocalDateTime AGORA = LocalDateTime.of(2026, 9, 1, 9, 0);

    @Autowired TestEntityManager entityManager;
    @Autowired VacationRequestRepository repository;

    private Employee ana;
    private Employee bruno;

    @BeforeEach
    void setUp() {
        ana = funcionario("1001", "ana@teste.com");
        bruno = funcionario("1002", "bruno@teste.com");
    }

    private Employee funcionario(String codigo, String email) {
        Employee e = new Employee();
        e.setCodParceiro(codigo);
        e.setAtivo(true);
        e.setEmail(new Email(email));
        return entityManager.persistAndFlush(e);
    }

    /** Férias de 01/10 a 10/10, no status pedido. */
    private void ferias(Employee e, VacationRequestStatus status) {
        VacationRequest r = VacationRequest.request(
                e, LocalDate.of(2026, 10, 1), LocalDate.of(2026, 10, 10), null, AGORA);
        if (status == APPROVED) r.approve(null, "ok", AGORA);
        if (status == VacationRequestStatus.REJECTED) r.reject(null, "conflito", AGORA);
        entityManager.persistAndFlush(r);
    }

    @Test
    @DisplayName("período que cruza férias aprovadas do próprio funcionário conflita")
    void cruzaAprovadas() {
        ferias(ana, APPROVED);

        assertThat(repository.existsOverlapForEmployee(
                ana, ABERTAS, LocalDate.of(2026, 10, 5), LocalDate.of(2026, 10, 15))).isTrue();
    }

    @Test
    @DisplayName("pedido pendente do próprio funcionário também conflita")
    void cruzaPendente() {
        ferias(ana, PENDING);

        assertThat(repository.existsOverlapForEmployee(
                ana, ABERTAS, LocalDate.of(2026, 10, 1), LocalDate.of(2026, 10, 10))).isTrue();
    }

    /** As pontas: terminar no dia em que as outras começam já é cruzar. */
    @Test
    @DisplayName("encostar no primeiro dia conta como cruzar")
    void encostarContaComoCruzar() {
        ferias(ana, APPROVED);

        assertThat(repository.existsOverlapForEmployee(
                ana, ABERTAS, LocalDate.of(2026, 9, 25), LocalDate.of(2026, 10, 1))).isTrue();
    }

    @Test
    @DisplayName("período logo depois não conflita")
    void periodoSeguinteNaoConflita() {
        ferias(ana, APPROVED);

        assertThat(repository.existsOverlapForEmployee(
                ana, ABERTAS, LocalDate.of(2026, 10, 11), LocalDate.of(2026, 10, 20))).isFalse();
    }

    @Test
    @DisplayName("férias de outro funcionário não contam — isso é a regra do setor")
    void outroFuncionarioNaoConta() {
        ferias(bruno, APPROVED);

        assertThat(repository.existsOverlapForEmployee(
                ana, ABERTAS, LocalDate.of(2026, 10, 1), LocalDate.of(2026, 10, 10))).isFalse();
    }

    @Test
    @DisplayName("pedido reprovado não ocupa o período")
    void reprovadoNaoConta() {
        ferias(ana, VacationRequestStatus.REJECTED);

        assertThat(repository.existsOverlapForEmployee(
                ana, ABERTAS, LocalDate.of(2026, 10, 1), LocalDate.of(2026, 10, 10))).isFalse();
    }

    /**
     * É o que a aprovação pede: só APPROVED. O próprio pedido sendo aprovado
     * ainda está PENDING e não pode conflitar consigo mesmo.
     */
    @Test
    @DisplayName("consultando só APPROVED, um pendente não conflita")
    void soAprovadasIgnoraPendente() {
        ferias(ana, PENDING);

        assertThat(repository.existsOverlapForEmployee(
                ana, List.of(APPROVED), LocalDate.of(2026, 10, 1), LocalDate.of(2026, 10, 10))).isFalse();
    }
}
