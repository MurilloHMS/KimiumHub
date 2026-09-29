package com.proautokimium.api.Infrastructure.services.humanResources;

import com.proautokimium.api.Application.DTOs.humanResources.EmployeeDocument.EmployeeDocumentTypeDTO;
import com.proautokimium.api.Application.DTOs.humanResources.EmployeeDocument.EmployeeDocumentTypeRequestDTO;
import com.proautokimium.api.Infrastructure.exceptions.humanResources.EmployeeDocumentTypeAlreadyExistsException;
import com.proautokimium.api.Infrastructure.repositories.EmployeeRepository;
import com.proautokimium.api.Infrastructure.repositories.humanResources.EmployeeDocumentTypeRepository;
import com.proautokimium.api.domain.entities.Employee;
import com.proautokimium.api.domain.entities.humanResources.EmployeeDocumentType;
import com.proautokimium.api.domain.exceptions.humanResources.InvalidRequestDataException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class EmployeeDocumentTypeServiceTest {

    @Mock private EmployeeDocumentTypeRepository repository;
    @Mock private EmployeeRepository employeeRepository;

    private EmployeeDocumentTypeService service;

    @BeforeEach
    void setUp() {
        Clock clock = Clock.fixed(Instant.parse("2026-09-29T13:00:00Z"), ZoneId.of("America/Sao_Paulo"));
        service = new EmployeeDocumentTypeService(repository, employeeRepository, clock);
    }

    private static EmployeeDocumentTypeRequestDTO request(String name, List<Integer> days, List<UUID> recipients) {
        return new EmployeeDocumentTypeRequestDTO(name, days, true, recipients, true);
    }

    /** 409 com mensagem, e não o erro de chave única do banco virando 500. */
    @Test
    @DisplayName("nome repetido, em qualquer caixa, é recusado antes do banco")
    void nomeRepetido() {
        when(repository.existsByNameIgnoreCase("aso")).thenReturn(true);

        assertThatThrownBy(() -> service.create(request(" aso ", List.of(30), List.of())))
                .isInstanceOf(EmployeeDocumentTypeAlreadyExistsException.class);
        verify(repository, never()).save(any());
    }

    @Test
    @DisplayName("responsável que não existe é recusado com 400")
    void responsavelInexistente() {
        UUID existe = UUID.randomUUID();
        UUID naoExiste = UUID.randomUUID();
        Employee employee = new Employee();
        employee.id = existe;
        when(employeeRepository.findAllById(Set.of(existe, naoExiste))).thenReturn(List.of(employee));

        assertThatThrownBy(() -> service.create(request("NR-35", List.of(60), List.of(existe, naoExiste))))
                .isInstanceOf(InvalidRequestDataException.class);
        verify(repository, never()).save(any());
    }

    /** A ordem em que os avisos chegam: 60 dias, depois 15. */
    @Test
    @DisplayName("os dias voltam do maior para o menor")
    void diasEmOrdem() {
        when(repository.save(any(EmployeeDocumentType.class))).thenAnswer(inv -> inv.getArgument(0));

        EmployeeDocumentTypeDTO dto = service.create(request("NR-35", List.of(15, 60, 30), List.of()));

        assertThat(dto.alertDaysBefore()).containsExactly(60, 30, 15);
    }

    @Test
    @DisplayName("renomear para o nome de outro tipo é recusado")
    void renomearParaExistente() {
        EmployeeDocumentType type = EmployeeDocumentType.create("ASO", null);
        type.id = UUID.randomUUID();
        when(repository.findById(type.getId())).thenReturn(Optional.of(type));
        when(repository.existsByNameIgnoreCaseAndIdNot("NR", type.getId())).thenReturn(true);

        assertThatThrownBy(() -> service.update(type.getId(), request("NR", List.of(), List.of())))
                .isInstanceOf(EmployeeDocumentTypeAlreadyExistsException.class);
        assertThat(type.getName()).isEqualTo("ASO");
    }

    @Test
    @DisplayName("excluir só desativa")
    void excluirDesativa() {
        EmployeeDocumentType type = EmployeeDocumentType.create("ASO", null);
        type.id = UUID.randomUUID();
        when(repository.findById(type.getId())).thenReturn(Optional.of(type));

        service.deactivate(type.getId());

        assertThat(type.isActive()).isFalse();
        verify(repository, never()).delete(any());
    }
}
