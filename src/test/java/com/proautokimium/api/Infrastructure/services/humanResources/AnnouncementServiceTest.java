package com.proautokimium.api.Infrastructure.services.humanResources;

import com.proautokimium.api.Application.DTOs.humanResources.Announcement.AnnouncementResponseDTO;
import com.proautokimium.api.Application.DTOs.humanResources.Announcement.CreateAnnouncementRequestDTO;
import com.proautokimium.api.Application.DTOs.humanResources.Notification.SendNotificationRequestDTO;
import com.proautokimium.api.Application.DTOs.humanResources.Notification.SendNotificationResponseDTO;
import com.proautokimium.api.Infrastructure.repositories.EmployeeRepository;
import com.proautokimium.api.Infrastructure.repositories.UserRepository;
import com.proautokimium.api.Infrastructure.repositories.humanResources.AnnouncementRepository;
import com.proautokimium.api.domain.entities.Employee;
import com.proautokimium.api.domain.entities.humanResources.Announcement;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.lang.reflect.Field;
import java.time.Clock;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AnnouncementServiceTest {

    @Mock private AnnouncementRepository repository;
    @Mock private EmployeeRepository employeeRepository;
    @Mock private UserRepository userRepository;
    @Mock private EmployeeNotificationService notificationService;

    private AnnouncementService service;
    private UUID publisherId;
    private Employee publisher;

    @BeforeEach
    void setUp() throws Exception {
        Clock clock = Clock.fixed(LocalDateTime.of(2026, 7, 23, 10, 0).atZone(ZoneId.systemDefault()).toInstant(), ZoneId.systemDefault());
        service = new AnnouncementService(repository, employeeRepository, userRepository, notificationService, clock);

        publisherId = UUID.randomUUID();
        publisher = new Employee();
        publisher.setName("RH da Proauto");
        Field field = com.proautokimium.api.domain.abstractions.Entity.class.getDeclaredField("id");
        field.setAccessible(true);
        field.set(publisher, publisherId);
    }

    @Test
    @DisplayName("Publicar salva o aviso e notifica todos os funcionários ativos")
    void publicarSalvaENotificaTodos() {
        String login = "rh.login";
        when(userRepository.findByLoginWithEmployee(login)).thenReturn(Optional.empty());
        when(employeeRepository.findByUsername(login)).thenReturn(Optional.of(publisher));
        when(repository.save(any(Announcement.class))).thenAnswer(inv -> inv.getArgument(0));
        when(notificationService.send(any())).thenReturn(new SendNotificationResponseDTO(10, 0));

        CreateAnnouncementRequestDTO dto = new CreateAnnouncementRequestDTO(
                "Mudança no horário de almoço", "A partir de agosto, o almoço passa a ser das 12h às 13h."
        );

        AnnouncementResponseDTO response = service.publish(dto, login);

        assertThat(response.title()).isEqualTo("Mudança no horário de almoço");
        assertThat(response.publishedByName()).isEqualTo("RH da Proauto");

        ArgumentCaptor<SendNotificationRequestDTO> captor = ArgumentCaptor.forClass(SendNotificationRequestDTO.class);
        verify(notificationService).send(captor.capture());

        SendNotificationRequestDTO sentRequest = captor.getValue();
        assertThat(sentRequest.employeeIds()).isNull(); // null = todos
        assertThat(sentRequest.title()).contains("Mudança no horário de almoço");
        assertThat(sentRequest.message()).isEqualTo(dto.content());
    }

    @Test
    @DisplayName("listAll retorna os avisos mapeados, mais recente primeiro (ordem já vem do repositório)")
    void listAllRetornaAvisosMapeados() throws Exception {
        Announcement announcement = new Announcement();
        announcement.setTitle("Aviso 1");
        announcement.setContent("Conteúdo");
        announcement.setPublishedBy(publisher);
        announcement.setPublishedAt(LocalDateTime.of(2026, 7, 20, 9, 0));

        when(repository.findAllByOrderByPublishedAtDesc()).thenReturn(List.of(announcement));

        List<AnnouncementResponseDTO> result = service.listAll();

        assertThat(result).hasSize(1);
        assertThat(result.get(0).title()).isEqualTo("Aviso 1");
        assertThat(result.get(0).publishedById()).isEqualTo(publisherId);
    }
}
