package com.proautokimium.api.Infrastructure.repositories;

import com.proautokimium.api.domain.entities.Employee;
import com.proautokimium.api.domain.entities.HoleriteDocumento;
import com.proautokimium.api.domain.entities.auth.User;
import com.proautokimium.api.domain.enums.UserRole;
import com.proautokimium.api.domain.valueObjects.Email;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.test.context.ActiveProfiles;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * "Já enviado" é o que trava o reenvio. Cancelado não pode contar: o índice
 * único (V80) já libera o lugar, e o envio continuava pulando a pessoa — um PLR
 * enviado errado e cancelado não tinha como ser reenviado pelo fluxo normal.
 */
@DataJpaTest
@ActiveProfiles("test")
class HoleriteDocumentoRepositoryTest {

    private static final LocalDate SETEMBRO = LocalDate.of(2026, 9, 1);

    @Autowired TestEntityManager em;
    @Autowired HoleriteDocumentoRepository repository;

    @Test
    @DisplayName("cancelado não conta como já enviado; o ativo conta; outro tipo não conta")
    void canceledDoesNotCount() {
        Employee ana = funcionario("Ana", "9001");
        Employee bia = funcionario("Bia", "9002");
        Employee caio = funcionario("Caio", "9003");
        User rh = em.persist(new User("rh.teste", "rh@t.com", "hash", List.of(UserRole.RH)));

        HoleriteDocumento errado = em.persist(new HoleriteDocumento(ana, SETEMBRO, "PLR", "plr.pdf", "9001/a.pdf"));
        errado.cancelar(rh, "Valor errado", LocalDateTime.of(2026, 10, 2, 9, 0));
        em.persist(new HoleriteDocumento(bia, SETEMBRO, "PLR", "plr.pdf", "9002/b.pdf"));
        em.persist(new HoleriteDocumento(caio, SETEMBRO, "SALARIO", "sal.pdf", "9003/c.pdf"));
        em.flush();

        assertThat(repository.findEmployeeIdsByCompetenciaAndTipo(SETEMBRO, "PLR"))
                .containsExactly(bia.getId());
    }

    private Employee funcionario(String nome, String cod) {
        Employee e = new Employee();
        e.setName(nome);
        e.setCodParceiro(cod);
        e.setAtivo(true);
        e.setEmail(new Email(cod + "@t.com"));
        return em.persist(e);
    }
}
