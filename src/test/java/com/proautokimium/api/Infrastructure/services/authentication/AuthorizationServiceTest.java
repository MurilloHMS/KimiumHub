package com.proautokimium.api.Infrastructure.services.authentication;

import com.proautokimium.api.Infrastructure.repositories.EmployeeRepository;
import com.proautokimium.api.Infrastructure.repositories.UserRepository;
import com.proautokimium.api.domain.entities.Employee;
import com.proautokimium.api.domain.entities.auth.User;
import com.proautokimium.api.domain.enums.UserRole;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

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

    @Test
    @DisplayName("Deve carregar o usuário pelo email")
    void shouldLoadUserByEmail(){
        UserRepository repository = mock(UserRepository.class);
        EmployeeRepository employeeRepository = mock(EmployeeRepository.class);
        AuthorizationService service = new AuthorizationService();
        service.repository = repository;
        service.employeeRepository = employeeRepository;

        User user = new User("admin", "admin@teste.com", "hash", List.of(UserRole.ADMIN));

        when(repository.findByEmail("admin@teste.com")).thenReturn(user);

        var result = service.loadUserByUsername("admin@teste.com");
        assertThat(result).isEqualTo(user);

        verify(repository).findByEmail("admin@teste.com");
        verify(repository, never()).findByLogin(any());
    }

    @Test
    @DisplayName("Deve carregar usuário pelo CPF")
    void shouldLoadUserByCPF(){
        UserRepository repository = mock(UserRepository.class);
        EmployeeRepository employeeRepository = mock(EmployeeRepository.class);
        AuthorizationService service = new AuthorizationService();
        service.repository = repository;
        service.employeeRepository = employeeRepository;

        Employee employee = mock(Employee.class);
        User user = new User("admin", "admin@teste.com", "hash", List.of(UserRole.ADMIN));
        user.setEmployee(employee);

        when(employee.getId()).thenReturn(UUID.randomUUID());
        when(employeeRepository.findByCpfDigits("12345678900")).thenReturn(Optional.of(employee));
        when(repository.findByEmployee_Id(employee.getId())).thenReturn(Optional.of(user));

        var result = service.loadUserByUsername("12345678900");

        assertThat(result).isEqualTo(user);
        verify(employeeRepository).findByCpfDigits("12345678900");
        verify(repository, never()).findByLogin(any());
    }
}