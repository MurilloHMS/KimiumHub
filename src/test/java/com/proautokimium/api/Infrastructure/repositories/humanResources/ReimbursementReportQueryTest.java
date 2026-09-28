package com.proautokimium.api.Infrastructure.repositories.humanResources;

import com.proautokimium.api.domain.entities.Employee;
import com.proautokimium.api.domain.entities.humanResources.Reimbursement;
import com.proautokimium.api.domain.valueObjects.Email;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static com.proautokimium.api.domain.enums.humanResources.ReimbursementStatus.APPROVED;
import static com.proautokimium.api.domain.enums.humanResources.ReimbursementStatus.PAID;
import static com.proautokimium.api.domain.enums.humanResources.ReimbursementStatus.PENDING;
import static org.assertj.core.api.Assertions.assertThat;

/** As consultas do comprovante, contra o H2: período pela despesa, status, ordem. */
@DataJpaTest
@ActiveProfiles("test")
class ReimbursementReportQueryTest {

    private static final LocalDate FROM = LocalDate.of(2026, 9, 1);
    private static final LocalDate TO = LocalDate.of(2026, 9, 30);

    @Autowired TestEntityManager em;
    @Autowired ReimbursementRepository repository;

    private Employee bruno;
    private Employee ana;
    private Employee rh;

    @BeforeEach
    void setUp() {
        bruno = funcionario("Bruno", "3001", "bruno@t.com");
        ana = funcionario("Ana", "3002", "ana@t.com");
        rh = funcionario("Carla RH", "3003", "carla@t.com");
    }

    private Employee funcionario(String nome, String codigo, String email) {
        Employee e = new Employee();
        e.setName(nome);
        e.setCodParceiro(codigo);
        e.setAtivo(true);
        e.setEmail(new Email(email));
        return em.persistAndFlush(e);
    }

    private Reimbursement pedido(Employee quem, LocalDate despesa, boolean aprovado) {
        Reimbursement r = Reimbursement.request(quem, despesa, new BigDecimal("10.00"), "Combustível", "x",
                "n.jpg", "p/n.jpg", LocalDateTime.of(2026, 9, 1, 9, 0));
        if (aprovado) r.approve(rh, "ok", LocalDateTime.of(2026, 9, 2, 9, 0));
        return em.persistAndFlush(r);
    }

    @Test
    @DisplayName("filtra pela data da DESPESA, inclusive nas pontas, e pelos status")
    void filtraPeriodoEStatus() {
        Reimbursement primeiroDia = pedido(ana, FROM, false);
        Reimbursement ultimoDia = pedido(bruno, TO, true);
        pedido(ana, FROM.minusDays(1), false);  // fora: antes
        pedido(ana, TO.plusDays(1), false);     // fora: depois
        Reimbursement aprovado = pedido(ana, LocalDate.of(2026, 9, 15), true);
        em.clear();

        List<Reimbursement> soPendentes = repository.findForReport(FROM, TO, List.of(PENDING));
        List<Reimbursement> abertos = repository.findForReport(FROM, TO, List.of(PENDING, APPROVED, PAID));

        assertThat(soPendentes).extracting(Reimbursement::getId).containsExactly(primeiroDia.getId());
        assertThat(abertos).extracting(Reimbursement::getId)
                .as("ordenado por nome do funcionário, depois pela data")
                .containsExactly(primeiroDia.getId(), aprovado.getId(), ultimoDia.getId());
    }

    @Test
    @DisplayName("por funcionário, só os dele — e o revisor vem carregado")
    void porFuncionario() {
        pedido(ana, LocalDate.of(2026, 9, 10), true);
        pedido(bruno, LocalDate.of(2026, 9, 10), true);
        em.clear();

        List<Reimbursement> daAna = repository.findForReportByEmployee(
                em.find(Employee.class, ana.getId()), FROM, TO, List.of(APPROVED));

        assertThat(daAna).hasSize(1);
        assertThat(daAna.get(0).getEmployee().getName()).isEqualTo("Ana");
        assertThat(daAna.get(0).getReviewedBy().getName()).isEqualTo("Carla RH");
    }
}
