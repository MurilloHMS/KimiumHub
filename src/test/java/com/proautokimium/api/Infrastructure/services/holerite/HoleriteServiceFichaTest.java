package com.proautokimium.api.Infrastructure.services.holerite;

import com.proautokimium.api.Application.DTOs.holerite.HoleriteAuditoriaDTO;
import com.proautokimium.api.Infrastructure.repositories.EmployeeRepository;
import com.proautokimium.api.Infrastructure.repositories.HoleriteDocumentoRepository;
import com.proautokimium.api.Infrastructure.repositories.PayslipTypeRepository;
import com.proautokimium.api.Infrastructure.repositories.UserRepository;
import com.proautokimium.api.Infrastructure.services.notification.NotificationService;
import com.proautokimium.api.Infrastructure.services.pdf.holerith.HolerithExtractorService;
import com.proautokimium.api.Infrastructure.services.storage.HoleriteStorageService;
import com.proautokimium.api.domain.entities.Employee;
import com.proautokimium.api.domain.entities.HoleriteDocumento;
import com.proautokimium.api.domain.entities.auth.User;
import com.proautokimium.api.domain.enums.UserRole;
import com.proautokimium.api.domain.exceptions.partners.EmployeeNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

/**
 * Os holerites de uma pessoa, para a ficha do funcionário (2026-10-05).
 *
 * O que importa: o cancelado aparece. A ficha é o histórico do RH, e um PLR
 * enviado errado e cancelado é exatamente o que alguém vai perguntar depois.
 */
class HoleriteServiceFichaTest {

    private final EmployeeRepository employees = mock(EmployeeRepository.class);
    private final HoleriteDocumentoRepository repository = mock(HoleriteDocumentoRepository.class);
    private final UserRepository users = mock(UserRepository.class);
    private HoleriteService service;

    @BeforeEach
    void setUp() {
        service = new HoleriteService(mock(HolerithExtractorService.class), mock(HoleriteStorageService.class), employees,
                repository, users, mock(NotificationService.class),
                new PayslipTypeService(mock(PayslipTypeRepository.class), Clock.systemDefaultZone()), Clock.systemDefaultZone());
    }

    @Test
    @DisplayName("devolve todos, cancelados incluídos, e diz se a pessoa tem login")
    void includesCanceled() {
        UUID id = UUID.randomUUID();
        Employee ana = new Employee();
        ana.setName("Ana");
        ana.setCodParceiro("9001");
        when(employees.findById(id)).thenReturn(Optional.of(ana));
        when(users.findByEmployee_Id(id)).thenReturn(Optional.of(new User("ana", "a@t.com", "x", List.of(UserRole.USER))));

        HoleriteDocumento valendo = new HoleriteDocumento(ana, LocalDate.of(2026, 9, 1), "SALARIO", "sal.pdf", "p1");
        HoleriteDocumento cancelado = new HoleriteDocumento(ana, LocalDate.of(2026, 9, 1), "PLR", "plr.pdf", "p2");
        cancelado.cancelar(new User("rh", "rh@t.com", "x", List.of(UserRole.RH)), "Valor errado", LocalDateTime.of(2026, 10, 2, 9, 0));
        when(repository.findByEmployeeOrderByCompetenciaDesc(ana)).thenReturn(List.of(valendo, cancelado));

        List<HoleriteAuditoriaDTO> lista = service.listarParaRh(id);

        assertThat(lista).extracting(HoleriteAuditoriaDTO::tipo).containsExactly("SALARIO", "PLR");
        assertThat(lista.get(1).canceledAt()).isNotNull();
        assertThat(lista.get(1).cancelReason()).isEqualTo("Valor errado");
        assertThat(lista).allMatch(HoleriteAuditoriaDTO::temUsuario);
        verify(users, times(1)).findByEmployee_Id(id);
        verify(repository, never()).findByEmployeeAndCanceledAtIsNullOrderByCompetenciaDesc(any());
    }

    @Test
    @DisplayName("funcionário que não existe: 404, e o repositório de holerites nem é consultado")
    void unknownEmployee() {
        UUID id = UUID.randomUUID();
        when(employees.findById(id)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.listarParaRh(id)).isInstanceOf(EmployeeNotFoundException.class);
        verifyNoInteractions(repository);
    }
}
