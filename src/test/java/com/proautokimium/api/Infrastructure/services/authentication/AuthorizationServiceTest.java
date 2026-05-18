package com.proautokimium.api.Infrastructure.services.authentication;

import com.proautokimium.api.Infrastructure.repositories.UserRepository;
import com.proautokimium.api.domain.entities.auth.User;
import com.proautokimium.api.domain.enums.UserRole;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

class AuthorizationServiceTest {

    @Test
    @DisplayName("Deve carregar usuário pelo login")
    void shouldLoadUserByUsername() {
        UserRepository repository = mock(UserRepository.class);
        AuthorizationService service = new AuthorizationService();

        User user = new User("admin", "admin@teste.com", "hash", List.of(UserRole.ADMIN));
        when(repository.findByLogin("admin")).thenReturn(user);

        service.repository = repository;

        var result = service.loadUserByUsername("admin");

        assertThat(result).isEqualTo(user);
        verify(repository).findByLogin("admin");
    }
}