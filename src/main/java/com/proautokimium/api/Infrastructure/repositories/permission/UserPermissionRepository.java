package com.proautokimium.api.Infrastructure.repositories.permission;

import com.proautokimium.api.domain.entities.permission.UserPermission;
import jakarta.transaction.Transactional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface UserPermissionRepository
        extends JpaRepository<UserPermission, UserPermission.Key> {

    /**
     * As authorities de uma pessoa, já no formato que o `@PreAuthorize` espera.
     *
     * Roda **uma vez por requisição**, no filtro de segurança. Devolve
     * `stock/movements:EXCLUIR` pronto, em vez de trazer as linhas e montar a
     * string no Java — assim o banco só manda o que é permitido, e não as 385
     * de cada pessoa.
     *
     * O índice parcial `ix_user_permissions_allowed` cobre exatamente isto.
     */
    @Query(value = """
        SELECT screen_code || ':' || permission
          FROM user_permissions
         WHERE user_id = :userId AND allowed
        """, nativeQuery = true)
    List<String> findAuthorities(@Param("userId") String userId);

    /** O grid de um usuário, na ordem das telas. */
    @Query("""
        SELECT up FROM UserPermission up
        WHERE up.userId = :userId
    """)
    List<UserPermission> findAllOfUser(@Param("userId") String userId);

    /**
     * O mesmo para as pessoas, e **cliente fica de fora**.
     *
     * O portal do cliente tem sessão e escopo próprios, decididos pela API.
     * Dar linhas de tela de ERP a um cliente seria dizer que ele participa
     * deste sistema, e ele não participa.
     *
     * Uma permissão por chamada, pelo mesmo motivo do repositório de modelo:
     * `UNNEST` com array é do Postgres, e os testes rodam em H2.
     */
    @Modifying
    @Transactional
    @Query(value = """
        INSERT INTO user_permissions (user_id, screen_code, permission, allowed)
        SELECT u.id, s.code, :permission, FALSE
          FROM users u
         CROSS JOIN screens s
         WHERE NOT EXISTS (
               SELECT 1 FROM user_roles r
                WHERE r.user_id = u.id AND r.role = 'CLIENTE'
         )
           AND NOT EXISTS (
               SELECT 1 FROM user_permissions x
                WHERE x.user_id = u.id
                  AND x.screen_code = s.code
                  AND x.permission  = :permission
         )
        """, nativeQuery = true)
    int createMissing(@Param("permission") String permission);
}
