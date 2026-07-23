package com.proautokimium.api.Infrastructure.services.humanResources;

import com.proautokimium.api.Application.DTOs.humanResources.CollectiveBargainingAdjustment.ApplyCollectiveBargainingAdjustmentRequestDTO;
import com.proautokimium.api.Application.DTOs.humanResources.CollectiveBargainingAdjustment.CollectiveBargainingAdjustmentResponseDTO;
import com.proautokimium.api.Infrastructure.repositories.humanResources.CareerHistoryRepository;
import com.proautokimium.api.Infrastructure.repositories.humanResources.CollectiveBargainingAdjustmentRepository;
import com.proautokimium.api.Infrastructure.repositories.humanResources.PositionLevelRepository;
import com.proautokimium.api.Infrastructure.repositories.humanResources.PositionRepository;
import com.proautokimium.api.domain.entities.Employee;
import com.proautokimium.api.domain.entities.humanResources.CareerHistory;
import com.proautokimium.api.domain.entities.humanResources.Position;
import com.proautokimium.api.domain.entities.humanResources.PositionLevel;
import com.proautokimium.api.domain.enums.humanResources.AdjustmentScope;
import com.proautokimium.api.domain.enums.humanResources.ContractType;
import com.proautokimium.api.domain.enums.humanResources.SalaryAdjustmentType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CollectiveBargainingAdjustmentServiceTest {

    @Mock private CollectiveBargainingAdjustmentRepository adjustmentRepository;
    @Mock private PositionRepository positionRepository;
    @Mock private PositionLevelRepository positionLevelRepository;
    @Mock private CareerHistoryRepository careerHistoryRepository;

    private CollectiveBargainingAdjustmentService service;

    private Position position;
    private PositionLevel junior;
    private PositionLevel pleno;
    private Employee employeeJunior;
    private Employee employeePleno;

    @BeforeEach
    void setUp() {
        // Resolver real (não mockado) — queremos provar a cascata de verdade, não simulá-la.
        PositionLevelSalaryResolver resolver = new PositionLevelSalaryResolver(
                positionLevelRepository, new FixedSalaryStrategy(), new PercentageSalaryStrategy()
        );
        service = new CollectiveBargainingAdjustmentService(
                adjustmentRepository, positionRepository, positionLevelRepository, careerHistoryRepository, resolver
        );

        position = new Position();
        position.setName("Desenvolvedor Java");

        junior = PositionLevel.fixed("Júnior", 1, position, new BigDecimal("1000.00"));
        pleno = PositionLevel.percentage("Pleno", 2, position, new BigDecimal("10"));

        employeeJunior = new Employee();
        employeePleno = new Employee();
    }

    @Test
    @DisplayName("Dissídio ALL_POSITIONS reajusta o nível FIXED e gera novo CareerHistory pro funcionário nele")
    void deveReajustarNivelFixoEGerarHistoricoParaFuncionarioNoNivelFixo() {
        CareerHistory latestJunior = new CareerHistory(
                employeeJunior, position, junior, new BigDecimal("1000.00"),
                ContractType.CLT, com.proautokimium.api.domain.enums.humanResources.CareerChangeReason.HIRING,
                LocalDate.of(2026, 1, 1), null
        );

        when(positionLevelRepository.findByAdjustmentType(SalaryAdjustmentType.FIXED)).thenReturn(List.of(junior));
        when(careerHistoryRepository.findLatestPerEmployee()).thenReturn(List.of(latestJunior));

        ApplyCollectiveBargainingAdjustmentRequestDTO request = new ApplyCollectiveBargainingAdjustmentRequestDTO(
                new BigDecimal("10"), LocalDate.of(2026, 8, 1), AdjustmentScope.ALL_POSITIONS, null
        );

        CollectiveBargainingAdjustmentResponseDTO response = service.apply(request);

        assertThat(response.positionLevelsUpdated()).isEqualTo(1);
        assertThat(response.employeesAffected()).isEqualTo(1);
        assertThat(junior.getFixedAmount()).isEqualByComparingTo("1100.00");

        ArgumentCaptor<CareerHistory> captor = ArgumentCaptor.forClass(CareerHistory.class);
        verify(careerHistoryRepository).save(captor.capture());
        assertThat(captor.getValue().getSalary()).isEqualByComparingTo("1100.00");
        assertThat(captor.getValue().getReason().name()).isEqualTo("COLLECTIVE_BARGAINING_ADJUSTMENT");
    }

    @Test
    @DisplayName("Funcionário em nível PERCENTAGE recebe o reajuste em cascata, mesmo sem o nível dele ser tocado diretamente")
    void deveCascatearReajusteParaFuncionarioEmNivelPercentual() {
        CareerHistory latestPleno = new CareerHistory(
                employeePleno, position, pleno, new BigDecimal("1100.00"),
                ContractType.CLT, com.proautokimium.api.domain.enums.humanResources.CareerChangeReason.HIRING,
                LocalDate.of(2026, 1, 1), null
        );

        when(positionLevelRepository.findByAdjustmentType(SalaryAdjustmentType.FIXED)).thenReturn(List.of(junior));
        when(careerHistoryRepository.findLatestPerEmployee()).thenReturn(List.of(latestPleno));
        when(positionLevelRepository.findByPositionAndLevelOrder(any(Position.class), eq(1))).thenReturn(java.util.Optional.of(junior));

        ApplyCollectiveBargainingAdjustmentRequestDTO request = new ApplyCollectiveBargainingAdjustmentRequestDTO(
                new BigDecimal("10"), LocalDate.of(2026, 8, 1), AdjustmentScope.ALL_POSITIONS, null
        );

        service.apply(request);

        // Júnior vai de 1000 pra 1100 (+10%); Pleno é Júnior * 1.10 = 1210 — cascata correta.
        ArgumentCaptor<CareerHistory> captor = ArgumentCaptor.forClass(CareerHistory.class);
        verify(careerHistoryRepository).save(captor.capture());
        assertThat(captor.getValue().getSalary()).isEqualByComparingTo("1210.00");
    }
}
