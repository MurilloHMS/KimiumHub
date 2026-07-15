package com.proautokimium.api.Infrastructure.repositories;

import com.proautokimium.api.domain.entities.auth.User;
import com.proautokimium.api.domain.enums.UserRole;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@ActiveProfiles("test")
class UserRoleMappingTest {
    @Autowired
    private TestEntityManager entityManager;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    @DisplayName("Role deve ser persistida como nome e não como número")
    void roleDeveSerPersistidaComoNomeENaoComoNumero(){
        // ARRANGE
        User user = new User("usuario.teste", "teste@teste.com", "Teste123@", List.of(UserRole.ADMIN));

        // ACT
        entityManager.persistAndFlush(user);

        // ASSERT
        String valorNoBanco = jdbcTemplate.queryForObject(
                "SELECT role FROM user_roles", String.class);
        assertThat(valorNoBanco).isEqualTo("ADMIN");
    }

}
