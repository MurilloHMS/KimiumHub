package com.proautokimium.api.Infrastructure.services.humanResources;

import com.proautokimium.api.Application.DTOs.humanResources.Announcement.AnnouncementResponseDTO;
import com.proautokimium.api.Application.DTOs.humanResources.Announcement.CreateAnnouncementRequestDTO;
import com.proautokimium.api.Application.DTOs.humanResources.Notification.SendNotificationRequestDTO;
import com.proautokimium.api.Infrastructure.repositories.EmployeeRepository;
import com.proautokimium.api.Infrastructure.repositories.UserRepository;
import com.proautokimium.api.Infrastructure.repositories.humanResources.AnnouncementRepository;
import com.proautokimium.api.domain.entities.Employee;
import com.proautokimium.api.domain.entities.humanResources.Announcement;
import com.proautokimium.api.domain.exceptions.partners.EmployeeNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;

@Service
public class AnnouncementService {

    private final AnnouncementRepository repository;
    private final EmployeeRepository employeeRepository;
    private final UserRepository userRepository;
    private final EmployeeNotificationService notificationService;
    private final Clock clock;

    public AnnouncementService(
            AnnouncementRepository repository,
            EmployeeRepository employeeRepository,
            UserRepository userRepository,
            EmployeeNotificationService notificationService,
            Clock clock
    ) {
        this.repository = repository;
        this.employeeRepository = employeeRepository;
        this.userRepository = userRepository;
        this.notificationService = notificationService;
        this.clock = clock;
    }

    /** Publica o aviso no mural e notifica todos os funcionários ativos. */
    @Transactional
    public AnnouncementResponseDTO publish(CreateAnnouncementRequestDTO dto, String login) {
        Employee publisher = resolveEmployee(login);
        if (publisher == null) {
            throw new EmployeeNotFoundException();
        }

        Announcement announcement = new Announcement();
        announcement.setTitle(dto.title());
        announcement.setContent(dto.content());
        announcement.setPublishedBy(publisher);
        announcement.setPublishedAt(LocalDateTime.now(clock));

        Announcement saved = repository.save(announcement);

        notificationService.send(new SendNotificationRequestDTO(
                null, "Novo aviso: " + dto.title(), dto.content(), "/mural"
        ));

        return toResponse(saved);
    }

    public List<AnnouncementResponseDTO> listAll() {
        return repository.findAllByOrderByPublishedAtDesc().stream()
                .map(this::toResponse)
                .toList();
    }

    private Employee resolveEmployee(String login) {
        Employee viaLink = userRepository.findByLoginWithEmployee(login)
                .map(u -> u.getEmployee())
                .orElse(null);
        if (viaLink != null) return viaLink;
        return employeeRepository.findByUsername(login).orElse(null);
    }

    private AnnouncementResponseDTO toResponse(Announcement announcement) {
        return new AnnouncementResponseDTO(
                announcement.getId(),
                announcement.getTitle(),
                announcement.getContent(),
                announcement.getPublishedBy().getId(),
                announcement.getPublishedBy().getName(),
                announcement.getPublishedAt()
        );
    }
}
