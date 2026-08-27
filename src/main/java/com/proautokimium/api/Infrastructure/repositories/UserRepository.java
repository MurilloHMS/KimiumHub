package com.proautokimium.api.Infrastructure.repositories;

import com.proautokimium.api.domain.entities.auth.User;
import com.proautokimium.api.domain.enums.UserRole;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface UserRepository extends JpaRepository<User, String> {
    UserDetails findByLogin(String login);
    UserDetails findByEmail(String email);

    /** Carrega o usuário já com o funcionário vinculado, evitando lazy loading fora da transação. */
    @Query("SELECT u FROM users u LEFT JOIN FETCH u.employee WHERE u.login = :login")
    Optional<User> findByLoginWithEmployee(@Param("login") String login);

    /** Lista todos os usuários já com o funcionário vinculado (evita N+1 e lazy loading). */
    @Query("SELECT u FROM users u LEFT JOIN FETCH u.employee")
    List<User> findAllWithEmployee();

    Optional<User> findByEmployee_Id(UUID employeeId);

    List<User> findByCustomer_Id(UUID customerId);

    /** Usuários que possuem qualquer uma das roles informadas (DISTINCT: usuário com mais de uma role vem uma vez só). */
    @Query("SELECT DISTINCT u FROM users u JOIN u.roles r WHERE r IN :roles")
    List<User> findByRolesIn(@Param("roles") Collection<UserRole> roles);

    /**
     * A conta é de desenvolvedor?
     *
     * Consulta nativa em `user_roles` porque `roles` é `@ElementCollection`:
     * carregar o `User` inteiro só para olhar uma role custaria um join a mais
     * numa consulta que roda **uma vez por requisição**.
     */
    @Query(value = """
        SELECT EXISTS (
            SELECT 1 FROM user_roles WHERE user_id = :userId AND role = 'DEVELOPER'
        )
        """, nativeQuery = true)
    boolean isDeveloper(@Param("userId") String userId);

    boolean existsByLogin(String username);

    @Query("select coalesce(e.name, u.login) from users u left join u.employee e where u.login = :login")
    Optional<String> findDisplayName(@Param("login") String login);

    /** Carrega o usuário já com o cliente, evitando lazy loading fora da transação. */
    @Query("SELECT u FROM users u LEFT JOIN FETCH u.customer WHERE u.login = :login")
    Optional<User> findByLoginWithCustomer(@Param("login") String login);
}
