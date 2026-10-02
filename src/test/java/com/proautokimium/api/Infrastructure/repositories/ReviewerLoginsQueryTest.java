package com.proautokimium.api.Infrastructure.repositories;

import com.proautokimium.api.domain.entities.auth.User;
import com.proautokimium.api.domain.entities.permission.UserPermission;
import com.proautokimium.api.domain.enums.UserRole;
import com.proautokimium.api.domain.enums.Permission;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.test.context.ActiveProfiles;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Quem recebe o sino de "atestado para conferir": quem tem a célula na grade —
 * não quem tem a role RH —, e só se a conta está ativa.
 */
@DataJpaTest
@ActiveProfiles("test")
class ReviewerLoginsQueryTest {

    @Autowired TestEntityManager em;
    @Autowired UserRepository repository;

    @Test
    @DisplayName("só quem tem a célula permitida, ativo, uma vez cada")
    void onlyAllowedAndActive() {
        User rita = user("rita", true);
        User bruno = user("bruno", true);
        User negado = user("negado", true);
        User inativo = user("inativo", false);
        User outraTela = user("outra", true);

        grant(rita, "rh/medical-certificates", Permission.ALTERAR, true);
        grant(rita, "rh/medical-certificates", Permission.CONSULTAR, true);
        grant(bruno, "rh/medical-certificates", Permission.ALTERAR, true);
        grant(negado, "rh/medical-certificates", Permission.ALTERAR, false);
        grant(inativo, "rh/medical-certificates", Permission.ALTERAR, true);
        grant(outraTela, "rh/reimbursements", Permission.ALTERAR, true);
        grant(outraTela, "rh/medical-certificates", Permission.CONSULTAR, true);
        em.flush();

        assertThat(repository.findActiveLoginsAllowed("rh/medical-certificates", "ALTERAR"))
                .containsExactlyInAnyOrder("rita", "bruno");
    }

    private User user(String login, boolean active) {
        User u = new User(login, login + "@t.com", "hash", List.of(UserRole.RH));
        u.setActive(active);
        return em.persist(u);
    }

    private void grant(User u, String screen, Permission permission, boolean allowed) {
        UserPermission p = new UserPermission();
        p.setUserId(u.getId());
        p.setScreenCode(screen);
        p.setPermission(permission);
        p.setAllowed(allowed);
        em.persist(p);
    }
}
