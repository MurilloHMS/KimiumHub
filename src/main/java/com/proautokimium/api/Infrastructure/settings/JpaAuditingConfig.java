package com.proautokimium.api.Infrastructure.settings;

import com.proautokimium.api.domain.entities.auth.User;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.domain.AuditorAware;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.Optional;

/**
 * Liga a auditoria do Spring Data e diz quem é o autor da alteração.
 *
 * O AuditorAware existe para a entidade não precisar conhecer o SecurityContext:
 * o domínio continua sem saber que existe autenticação.
 */
@Configuration
@EnableJpaAuditing(auditorAwareRef = "auditorProvider")
public class JpaAuditingConfig {

    @Bean
    public AuditorAware<String> auditorProvider() {
        return () -> Optional.of(currentUser());
    }

    private String currentUser() {
        var authentication = SecurityContextHolder.getContext().getAuthentication();

        if (authentication == null || !authentication.isAuthenticated()) return "Sistema";

        if (authentication.getPrincipal() instanceof User user) return user.getLogin();

        return "Sistema";
    }
}