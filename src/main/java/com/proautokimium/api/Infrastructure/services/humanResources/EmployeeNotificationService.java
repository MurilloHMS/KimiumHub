package com.proautokimium.api.Infrastructure.services.humanResources;

import com.proautokimium.api.Application.DTOs.humanResources.Notification.SendNotificationRequestDTO;
import com.proautokimium.api.Application.DTOs.humanResources.Notification.SendNotificationResponseDTO;
import com.proautokimium.api.Infrastructure.repositories.EmployeeRepository;
import com.proautokimium.api.Infrastructure.repositories.UserRepository;
import com.proautokimium.api.Infrastructure.services.notification.NotificationService;
import com.proautokimium.api.domain.entities.Employee;
import com.proautokimium.api.domain.entities.auth.User;
import com.proautokimium.api.domain.enums.NotificationType;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
public class EmployeeNotificationService {

    private final EmployeeRepository employeeRepository;
    private final UserRepository userRepository;
    private final NotificationService notificationService;

    public EmployeeNotificationService(
            EmployeeRepository employeeRepository,
            UserRepository userRepository,
            NotificationService notificationService
    ) {
        this.employeeRepository = employeeRepository;
        this.userRepository = userRepository;
        this.notificationService = notificationService;
    }

    /** RH manda uma mensagem personalizada pra um ou mais funcionários (ou todos, se employeeIds vier vazio). */
    @Transactional
    public SendNotificationResponseDTO send(SendNotificationRequestDTO dto) {
        List<Employee> targets = (dto.employeeIds() == null || dto.employeeIds().isEmpty())
                ? employeeRepository.findByAtivoTrue()
                : employeeRepository.findAllById(dto.employeeIds());

        int notified = 0;
        int skippedNoAccount = 0;

        for (Employee employee : targets) {
            Optional<User> user = userRepository.findByEmployee_Id(employee.getId());
            if (user.isEmpty()) {
                skippedNoAccount++;
                continue;
            }
            notificationService.notify(user.get().getLogin(), NotificationType.PERSONALIZADA,
                    dto.title(), dto.message(), dto.link());
            notified++;
        }

        return new SendNotificationResponseDTO(notified, skippedNoAccount);
    }
}
