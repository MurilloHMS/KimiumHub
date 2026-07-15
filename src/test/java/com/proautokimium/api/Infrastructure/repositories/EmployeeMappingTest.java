package com.proautokimium.api.Infrastructure.repositories;

import com.proautokimium.api.domain.entities.Employee;
import com.proautokimium.api.domain.enums.Hierarchy;
import com.proautokimium.api.domain.valueObjects.Email;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@ActiveProfiles("test")
class EmployeeMappingTest {

    @Autowired
    private TestEntityManager entityManager;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    @DisplayName("Hierarquia deve ser persistida como nome não como número")
    void shouldPersistEnumAsNameNotOrdinal(){
        Employee employee = new Employee();
        employee.setCodParceiro("123");
        employee.setAtivo(true);
        employee.setEmail(new Email("teste@teste.com"));
        employee.setHierarquia(Hierarchy.CEO);

        entityManager.persistAndFlush(employee);

        String hierarchy = jdbcTemplate.queryForObject(
                "SELECT hierarquia FROM parceiros", String.class
        );

        assertThat(hierarchy).isEqualTo("CEO");
    }
}
