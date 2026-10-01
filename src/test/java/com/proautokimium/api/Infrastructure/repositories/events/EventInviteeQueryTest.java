package com.proautokimium.api.Infrastructure.repositories.events;

import com.proautokimium.api.Infrastructure.repositories.EmployeeRepository;
import com.proautokimium.api.domain.entities.Employee;
import com.proautokimium.api.domain.entities.auth.User;
import com.proautokimium.api.domain.entities.events.CompanyEvent;
import com.proautokimium.api.domain.entities.humanResources.Company;
import com.proautokimium.api.domain.entities.humanResources.Department;
import com.proautokimium.api.domain.entities.humanResources.Team;
import com.proautokimium.api.domain.enums.UserRole;
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
import java.time.LocalTime;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Quem é convidado de um evento — a consulta contra o banco (H2), com as
 * quatro portas de entrada e as pessoas que nunca entram.
 *
 * <p>O caso que mais importa: <b>Juliana não tem equipe</b> e foi escolhida pelo
 * nome. Ela tem de continuar na lista — com um INNER JOIN na equipe, sumiria
 * sem erro nenhum. (O Hibernate 6.6 já gera LEFT JOIN até para o caminho com
 * ponto; a consulta escreve o LEFT JOIN para não depender disso.)
 */
@DataJpaTest
@ActiveProfiles("test")
class EventInviteeQueryTest {

    @Autowired TestEntityManager em;
    @Autowired EmployeeRepository employees;
    @Autowired CompanyEventRepository events;

    Company matriz, filial;
    Department comercial, tecnico;
    Employee diego, carlos, ana, juliana, pedroInativo, semLogin, loginBloqueado;
    int seq;

    @BeforeEach
    void setUp() {
        matriz = empresa("Matriz", "11.222.333/0001-81");
        filial = empresa("Filial", "11.222.333/0002-62");
        comercial = setor("Comercial");
        tecnico = setor("Técnico");
        Team timeComercial = em.persist(new Team("Vendas", comercial));
        Team timeTecnico = em.persist(new Team("Campo", tecnico));

        diego = funcionario("Diego", matriz, timeComercial, true, true);
        carlos = funcionario("Carlos", matriz, timeTecnico, true, true);
        ana = funcionario("Ana", filial, null, true, true);
        juliana = funcionario("Juliana", filial, null, true, true);
        pedroInativo = funcionario("Pedro", matriz, timeComercial, false, true);
        semLogin = funcionario("Sem Login", matriz, timeComercial, true, null);
        loginBloqueado = funcionario("Bloqueado", matriz, timeComercial, true, false);
        em.flush();
    }

    // ── As quatro portas ─────────────────────────────────────────────────────

    @Test
    @DisplayName("todos: os ativos com login ativo — nunca o inativo, o sem login nem o bloqueado")
    void everyone() {
        CompanyEvent e = evento(true, Set.of(), Set.of(), Set.of());

        assertThat(convidados(e)).containsExactly("Ana", "Carlos", "Diego", "Juliana");
    }

    @Test
    @DisplayName("pela empresa: todo funcionário ativo dela")
    void byCompany() {
        CompanyEvent e = evento(false, Set.of(matriz), Set.of(), Set.of());

        assertThat(convidados(e)).containsExactly("Carlos", "Diego");
    }

    @Test
    @DisplayName("pelo setor: pela equipe de cada um")
    void byDepartment() {
        CompanyEvent e = evento(false, Set.of(), Set.of(comercial), Set.of());

        assertThat(convidados(e)).containsExactly("Diego");
    }

    @Test
    @DisplayName("pelo nome: quem não tem equipe também entra (o LEFT JOIN)")
    void byNameWithoutTeam() {
        CompanyEvent e = evento(false, Set.of(), Set.of(), Set.of(juliana));

        assertThat(convidados(e)).containsExactly("Juliana");
    }

    @Test
    @DisplayName("setor + nome de quem não tem equipe: os dois entram, nenhum se perde")
    void departmentPlusSomeoneWithoutTeam() {
        CompanyEvent e = evento(false, Set.of(), Set.of(comercial), Set.of(juliana));

        assertThat(convidados(e)).containsExactly("Diego", "Juliana");
    }

    @Test
    @DisplayName("convidado por duas portas aparece uma vez só")
    void noDuplicates() {
        CompanyEvent e = evento(false, Set.of(matriz), Set.of(comercial), Set.of(diego));

        assertThat(convidados(e)).containsExactly("Carlos", "Diego");
    }

    @Test
    @DisplayName("escolher sem marcar nada: ninguém (e não todo mundo)")
    void emptyAudienceInvitesNobody() {
        CompanyEvent e = evento(false, Set.of(), Set.of(), Set.of());

        assertThat(convidados(e)).isEmpty();
    }

    // ── Uma pessoa só, e o lado do funcionário ───────────────────────────────

    @Test
    @DisplayName("isInvitedToEvent segue a mesma regra")
    void isInvitedFollowsTheSameRule() {
        CompanyEvent e = evento(false, Set.of(), Set.of(comercial), Set.of(juliana));

        assertThat(employees.isInvitedToEvent(e.getId(), diego.getId())).isTrue();
        assertThat(employees.isInvitedToEvent(e.getId(), juliana.getId())).isTrue();
        assertThat(employees.isInvitedToEvent(e.getId(), carlos.getId())).isFalse();
        assertThat(employees.isInvitedToEvent(e.getId(), loginBloqueado.getId())).as("login bloqueado").isFalse();
    }

    @Test
    @DisplayName("meus convites: só os eventos publicados em que a pessoa entra")
    void myInvitations() {
        CompanyEvent paraTodos = evento(true, Set.of(), Set.of(), Set.of());
        CompanyEvent soJuliana = evento(false, Set.of(), Set.of(), Set.of(juliana));
        CompanyEvent soMatriz = evento(false, Set.of(matriz), Set.of(), Set.of());
        CompanyEvent rascunho = evento(true, Set.of(), Set.of(), Set.of());
        rascunho.setPublishedAt(null);
        em.flush();
        em.clear();

        assertThat(events.findInvitationsFor(juliana.getId())).extracting(CompanyEvent::getId)
                .containsExactlyInAnyOrder(paraTodos.getId(), soJuliana.getId())
                .doesNotContain(soMatriz.getId(), rascunho.getId());
    }

    @Test
    @DisplayName("candidatos ao lembrete: publicados, lembrete ligado, e ainda não passou do primeiro dia")
    void reminderCandidates() {
        LocalDate hoje = LocalDate.of(2026, 10, 1);
        CompanyEvent ligado = evento(true, Set.of(), Set.of(), Set.of());
        ligado.setReminderEnabled(true);
        ligado.setReminderTime(LocalTime.of(9, 0));
        CompanyEvent desligado = evento(true, Set.of(), Set.of(), Set.of());
        CompanyEvent passado = evento(true, Set.of(), Set.of(), Set.of());
        passado.setReminderEnabled(true);
        passado.setReminderTime(LocalTime.of(9, 0));
        passado.setStartDate(LocalDate.of(2026, 9, 1));
        passado.setEndDate(LocalDate.of(2026, 9, 2));
        em.flush();
        em.clear();

        assertThat(events.findReminderCandidates(hoje)).extracting(CompanyEvent::getId)
                .containsExactly(ligado.getId())
                .doesNotContain(desligado.getId(), passado.getId());
    }

    // ── Apoio ────────────────────────────────────────────────────────────────

    List<String> convidados(CompanyEvent e) {
        em.flush();
        em.clear();
        return employees.findEventInvitees(e.getId()).stream().map(Employee::getName).toList();
    }

    Company empresa(String nome, String cnpj) {
        Company c = new Company();
        c.setName(nome);
        c.setLegalName(nome + " Ltda");
        c.setCnpj(cnpj);
        return em.persist(c);
    }

    Department setor(String nome) {
        Department d = new Department();
        d.setName(nome);
        return em.persist(d);
    }

    /** {@code loginAtivo} nulo = sem usuário nenhum. */
    Employee funcionario(String nome, Company empresa, Team equipe, boolean ativo, Boolean loginAtivo) {
        seq++;
        Employee e = new Employee();
        e.setName(nome);
        e.setCodParceiro(String.valueOf(4000 + seq));
        e.setAtivo(ativo);
        e.setEmail(new Email("f" + seq + "@t.com"));
        e.setCompany(empresa);
        e.setTeam(equipe);
        em.persist(e);
        if (loginAtivo != null) {
            User u = new User("login" + seq, "u" + seq + "@t.com", "hash", List.of(UserRole.USER));
            u.setEmployee(e);
            u.setActive(loginAtivo);
            em.persist(u);
        }
        return e;
    }

    CompanyEvent evento(boolean todos, Set<Company> empresas, Set<Department> setores, Set<Employee> pessoas) {
        CompanyEvent e = new CompanyEvent();
        e.setName("Evento " + (++seq));
        e.setStartDate(LocalDate.of(2026, 10, 6));
        e.setEndDate(LocalDate.of(2026, 10, 8));
        e.setCreatedAt(LocalDateTime.of(2026, 9, 20, 9, 0));
        e.setPublishedAt(LocalDateTime.of(2026, 9, 20, 9, 0));
        e.setAudienceAll(todos);
        e.getAudienceCompanies().addAll(empresas);
        e.getAudienceDepartments().addAll(setores);
        e.getAudienceEmployees().addAll(pessoas);
        return em.persist(e);
    }
}
