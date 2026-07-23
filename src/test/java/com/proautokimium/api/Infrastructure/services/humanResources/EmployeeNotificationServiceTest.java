package com.proautokimium.api.Infrastructure.services.humanResources;

import com.proautokimium.api.Application.DTOs.humanResources.Notification.SendNotificationRequestDTO;
import com.proautokimium.api.Application.DTOs.humanResources.Notification.SendNotificationResponseDTO;
import com.proautokimium.api.Infrastructure.repositories.EmployeeRepository;
import com.proautokimium.api.Infrastructure.repositories.UserRepository;
import com.proautokimium.api.Infrastructure.services.notification.NotificationService;
import com.proautokimium.api.domain.entities.Employee;
import com.proautokimium.api.domain.entities.auth.User;
import com.proautokimium.api.domain.enums.NotificationType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.lang.reflect.Field;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class EmployeeNotificationServiceTest {

    @Mock private EmployeeRepository employeeRepository;
    @Mock private UserRepository userRepository;
    @Mock private NotificationService notificationService;

    private EmployeeNotificationService service;

    @BeforeEach
    void setUp() {
        service = new EmployeeNotificationService(employeeRepository, userRepository, notificationService);
    }

    private Employee employee(String name) throws Exception {
        Employee e = new Employee();
        e.setName(name);
        Field field = com.proautokimium.api.domain.abstractions.Entity.class.getDeclaredField("id");
        field.setAccessible(true);
        field.set(e, UUID.randomUUID());
        return e;
    }

    @Test
    @DisplayName("employeeIds vazio/nulo notifica todos os funcionários ativos")
    void listaVaziaNotificaTodosOsAtivos() throws Exception {
        Employee emp1 = employee("Murillo");
        Employee emp2 = employee("Lucas");

        User user1 = mock(User.class);
        when(user1.getLogin()).thenReturn("murillo.login");
        User user2 = mock(User.class);
        when(user2.getLogin()).thenReturn("lucas.login");

        when(employeeRepository.findByAtivoTrue()).thenReturn(List.of(emp1, emp2));
        when(userRepository.findByEmployee_Id(emp1.getId())).thenReturn(Optional.of(user1));
        when(userRepository.findByEmployee_Id(emp2.getId())).thenReturn(Optional.of(user2));

        SendNotificationRequestDTO dto = new SendNotificationRequestDTO(null, "Aviso", "Mensagem geral", null);

        SendNotificationResponseDTO response = service.send(dto);

        assertThat(response.notified()).isEqualTo(2);
        assertThat(response.skippedNoAccount()).isZero();
        verify(employeeRepository).findByAtivoTrue();
        verify(employeeRepository, never()).findAllById(any());
        verify(notificationService).notify(eq("murillo.login"), eq(NotificationType.PERSONALIZADA), eq("Aviso"), eq("Mensagem geral"), isNull());
        verify(notificationService).notify(eq("lucas.login"), eq(NotificationType.PERSONALIZADA), eq("Aviso"), eq("Mensagem geral"), isNull());
    }

    @Test
    @DisplayName("employeeIds informado notifica só os selecionados")
    void listaInformadaNotificaSoOsSelecionados() throws Exception {
        Employee emp1 = employee("Ana");
        UUID emp1Id = emp1.getId();

        User user1 = mock(User.class);
        when(user1.getLogin()).thenReturn("ana.login");

        when(employeeRepository.findAllById(List.of(emp1Id))).thenReturn(List.of(emp1));
        when(userRepository.findByEmployee_Id(emp1Id)).thenReturn(Optional.of(user1));

        SendNotificationRequestDTO dto = new SendNotificationRequestDTO(List.of(emp1Id), "Aviso", "Mensagem individual", "/link");

        SendNotificationResponseDTO response = service.send(dto);

        assertThat(response.notified()).isEqualTo(1);
        verify(employeeRepository, never()).findByAtivoTrue();
        verify(notificationService).notify("ana.login", NotificationType.PERSONALIZADA, "Aviso", "Mensagem individual", "/link");
    }

    @Test
    @DisplayName("Funcionário sem usuário vinculado é pulado e contado separadamente")
    void funcionarioSemUsuarioEhPulado() throws Exception {
        Employee comConta = employee("Com Conta");
        Employee semConta = employee("Sem Conta");

        User user = mock(User.class);
        when(user.getLogin()).thenReturn("com.conta.login");

        when(employeeRepository.findByAtivoTrue()).thenReturn(List.of(comConta, semConta));
        when(userRepository.findByEmployee_Id(comConta.getId())).thenReturn(Optional.of(user));
        when(userRepository.findByEmployee_Id(semConta.getId())).thenReturn(Optional.empty());

        SendNotificationRequestDTO dto = new SendNotificationRequestDTO(null, "Aviso", "Mensagem", null);

        SendNotificationResponseDTO response = service.send(dto);

        assertThat(response.notified()).isEqualTo(1);
        assertThat(response.skippedNoAccount()).isEqualTo(1);
        verify(notificationService, times(1)).notify(any(), any(), any(), any(), any());
    }
}
