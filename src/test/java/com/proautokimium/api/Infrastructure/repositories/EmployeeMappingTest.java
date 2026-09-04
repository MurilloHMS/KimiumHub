package com.proautokimium.api.Infrastructure.repositories;

import com.proautokimium.api.domain.entities.Employee;
import com.proautokimium.api.domain.entities.humanResources.Hierarchy;
import com.proautokimium.api.domain.valueObjects.Email;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.UUID;
import org.springframework.test.context.ActiveProfiles;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@ActiveProfiles("test")
class EmployeeMappingTest {

    @Autowired
    private TestEntityManager entityManager;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    /**
     * **A premissa deste teste mudou de coluna.**
     *
     * Ele nasceu guardando um defeito real: a hierarquia era gravada como
     * numero ordinal, e a V53 teve que converter os ordinais para nome. Agora
     * a hierarquia e uma FK para `hierarchies`, entao a coluna `hierarquia`
     * nao recebe mais nada — verificar o texto dela passaria a nao provar
     * coisa nenhuma.
     *
     * O que continua valendo a pena provar e que a associacao chega ao banco:
     * o que o Employee aponta e o que fica gravado em `hierarchy_id`.
     */
    @Test
    @DisplayName("Hierarquia deve ser persistida como FK para hierarchies")
    void shouldPersistHierarchyAsForeignKey(){
        Hierarchy ceo = new Hierarchy();
        ceo.setName("CEO");
        ceo.setLevelOrder(2);
        entityManager.persistAndFlush(ceo);

        Employee employee = new Employee();
        employee.setCodParceiro("123");
        employee.setAtivo(true);
        employee.setEmail(new Email("teste@teste.com"));
        employee.setHierarquia(ceo);

        entityManager.persistAndFlush(employee);

        UUID hierarchyId = jdbcTemplate.queryForObject(
                "SELECT hierarchy_id FROM parceiros", UUID.class
        );

        assertThat(hierarchyId).isEqualTo(ceo.getId());
    }
}
