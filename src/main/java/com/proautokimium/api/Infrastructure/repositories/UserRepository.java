package com.proautokimium.api.Infrastructure.repositories;

import com.proautokimium.api.domain.entities.auth.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface UserRepository extends JpaRepository<User, String> {
    UserDetails findByLogin(String login);

    /** Carrega o usuário já com o funcionário vinculado, evitando lazy loading fora da transação. */
    @Query("SELECT u FROM users u LEFT JOIN FETCH u.employee WHERE u.login = :login")
    Optional<User> findByLoginWithEmployee(@Param("login") String login);

    /** Lista todos os usuários já com o funcionário vinculado (evita N+1 e lazy loading). */
    @Query("SELECT u FROM users u LEFT JOIN FETCH u.employee")
    List<User> findAllWithEmployee();

    Optional<User> findByEmployee_Id(UUID employeeId);
}
