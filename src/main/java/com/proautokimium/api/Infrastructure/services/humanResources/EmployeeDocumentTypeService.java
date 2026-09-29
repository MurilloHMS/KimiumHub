package com.proautokimium.api.Infrastructure.services.humanResources;

import com.proautokimium.api.Application.DTOs.humanResources.EmployeeDocument.EmployeeDocumentTypeDTO;
import com.proautokimium.api.Application.DTOs.humanResources.EmployeeDocument.EmployeeDocumentTypeRequestDTO;
import com.proautokimium.api.Infrastructure.exceptions.humanResources.EmployeeDocumentTypeAlreadyExistsException;
import com.proautokimium.api.Infrastructure.exceptions.humanResources.EmployeeDocumentTypeNotFoundException;
import com.proautokimium.api.Infrastructure.repositories.EmployeeRepository;
import com.proautokimium.api.Infrastructure.repositories.humanResources.EmployeeDocumentTypeRepository;
import com.proautokimium.api.domain.entities.humanResources.EmployeeDocumentType;
import com.proautokimium.api.domain.exceptions.humanResources.InvalidRequestDataException;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.*;

/**
 * Os tipos de documento e os seus avisos.
 *
 * Excluir é DESATIVAR: um tipo com documentos vinculados não pode sumir, e um
 * tipo inativo só deixa de aparecer para documento novo.
 */
@Service
public class EmployeeDocumentTypeService {

    private final EmployeeDocumentTypeRepository repository;
    private final EmployeeRepository employeeRepository;
    private final Clock clock;

    public EmployeeDocumentTypeService(EmployeeDocumentTypeRepository repository,
                                       EmployeeRepository employeeRepository,
                                       Clock clock) {
        this.repository = repository;
        this.employeeRepository = employeeRepository;
        this.clock = clock;
    }

    @Transactional(readOnly = true)
    public List<EmployeeDocumentTypeDTO> list() {
        return repository.findAllByOrderByNameAsc().stream().map(this::toDto).toList();
    }

    @Transactional
    public EmployeeDocumentTypeDTO create(EmployeeDocumentTypeRequestDTO dto) {
        if (repository.existsByNameIgnoreCase(dto.name().trim())) {
            throw new EmployeeDocumentTypeAlreadyExistsException();
        }
        EmployeeDocumentType type = EmployeeDocumentType.create(dto.name(), LocalDateTime.now(clock));
        apply(type, dto);
        return toDto(repository.save(type));
    }

    @Transactional
    public EmployeeDocumentTypeDTO update(UUID id, EmployeeDocumentTypeRequestDTO dto) {
        EmployeeDocumentType type = repository.findById(id).orElseThrow(EmployeeDocumentTypeNotFoundException::new);
        if (repository.existsByNameIgnoreCaseAndIdNot(dto.name().trim(), id)) {
            throw new EmployeeDocumentTypeAlreadyExistsException();
        }
        type.rename(dto.name());
        apply(type, dto);
        return toDto(type);
    }

    @Transactional
    public void deactivate(UUID id) {
        repository.findById(id).orElseThrow(EmployeeDocumentTypeNotFoundException::new).deactivate();
    }

    /**
     * Responsável que não existe é recusado com 400 aqui — sem isto, a FK do
     * banco recusaria no commit, e a tela receberia um 500 sem explicação.
     */
    private void apply(EmployeeDocumentType type, EmployeeDocumentTypeRequestDTO dto) {
        Set<UUID> recipients = dto.recipientEmployeeIds() == null ? Set.of() : new HashSet<>(dto.recipientEmployeeIds());
        if (!recipients.isEmpty() && employeeRepository.findAllById(recipients).size() != recipients.size()) {
            throw new InvalidRequestDataException("Um dos responsáveis não foi encontrado.");
        }
        type.configureAlerts(dto.alertDaysBefore(), dto.notifyOnExpiry(), recipients);
        if (dto.active()) type.activate(); else type.deactivate();
    }

    private EmployeeDocumentTypeDTO toDto(EmployeeDocumentType type) {
        return new EmployeeDocumentTypeDTO(
                type.getId(),
                type.getName(),
                type.getAlertDaysBefore().stream().sorted(Comparator.reverseOrder()).toList(),
                type.isNotifyOnExpiry(),
                List.copyOf(type.getRecipientEmployeeIds()),
                type.isActive()
        );
    }
}
