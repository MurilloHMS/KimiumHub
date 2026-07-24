package com.proautokimium.api.Infrastructure.services.humanResources;

import com.proautokimium.api.Application.DTOs.humanResources.Calculator.CltPjComparisonResponseDTO;
import com.proautokimium.api.Application.DTOs.humanResources.Calculator.FuelRequestDTO;
import com.proautokimium.api.Application.DTOs.humanResources.Calculator.FuelResponseDTO;
import com.proautokimium.api.Application.DTOs.humanResources.Calculator.MealVoucherRequestDTO;
import com.proautokimium.api.Application.DTOs.humanResources.Calculator.MealVoucherResponseDTO;
import com.proautokimium.api.Application.DTOs.humanResources.Calculator.TransportationVoucherRequestDTO;
import com.proautokimium.api.Application.DTOs.humanResources.Calculator.TransportationVoucherResponseDTO;
import com.proautokimium.api.Infrastructure.exceptions.humanResources.CareerHistoryNotFoundException;
import com.proautokimium.api.Infrastructure.exceptions.humanResources.EmployeePayrollDataMissingException;
import com.proautokimium.api.Infrastructure.repositories.EmployeeRepository;
import com.proautokimium.api.Infrastructure.repositories.humanResources.CareerHistoryRepository;
import com.proautokimium.api.domain.entities.Employee;
import com.proautokimium.api.domain.entities.humanResources.CareerHistory;
import com.proautokimium.api.domain.entities.humanResources.Position;
import com.proautokimium.api.domain.entities.humanResources.PositionLevel;
import com.proautokimium.api.domain.enums.humanResources.CareerChangeReason;
import com.proautokimium.api.domain.enums.humanResources.ContractType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.lang.reflect.Field;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PayrollCalculatorServiceTest {

    @Mock private EmployeeRepository employeeRepository;
    @Mock private CareerHistoryRepository careerHistoryRepository;

    private PayrollCalculatorService service;
    private UUID employeeId;
    private Employee employee;

    @BeforeEach
    void setUp() throws Exception {
        service = new PayrollCalculatorService(employeeRepository, careerHistoryRepository);

        employeeId = UUID.randomUUID();
        employee = new Employee();
        employee.setName("Murillo");
        Field field = com.proautokimium.api.domain.abstractions.Entity.class.getDeclaredField("id");
        field.setAccessible(true);
        field.set(employee, employeeId);
    }

    @Test
    @DisplayName("Vale-transporte: total = valor da condução x quantidade x dias trabalhados")
    void calculaValeTransporte() {
        employee.setDailyCommutesCount(2);
        when(employeeRepository.findById(employeeId)).thenReturn(Optional.of(employee));

        TransportationVoucherResponseDTO response = service.calculateTransportationVoucher(
                new TransportationVoucherRequestDTO(employeeId, new BigDecimal("4.50"), 22)
        );

        assertThat(response.totalAmount()).isEqualByComparingTo("198.00");
    }

    @Test
    @DisplayName("Vale-transporte lança exceção se o funcionário não tem conduções cadastradas")
    void valeTransporteSemConducoesCadastradas() {
        when(employeeRepository.findById(employeeId)).thenReturn(Optional.of(employee));

        assertThrows(EmployeePayrollDataMissingException.class, () -> service.calculateTransportationVoucher(
                new TransportationVoucherRequestDTO(employeeId, new BigDecimal("4.50"), 22)
        ));
    }

    @Test
    @DisplayName("Vale-alimentação: total = valor da refeição x quantidade x dias trabalhados")
    void calculaValeAlimentacao() {
        employee.setDailyMealsCount(1);
        when(employeeRepository.findById(employeeId)).thenReturn(Optional.of(employee));

        MealVoucherResponseDTO response = service.calculateMealVoucher(
                new MealVoucherRequestDTO(employeeId, new BigDecimal("25.00"), 22)
        );

        assertThat(response.totalAmount()).isEqualByComparingTo("550.00");
    }

    @Test
    @DisplayName("Combustível: litros = km ÷ consumo; total = litros x preço do litro")
    void calculaCombustivel() {
        when(employeeRepository.findById(employeeId)).thenReturn(Optional.of(employee));

        FuelResponseDTO response = service.calculateFuel(
                new FuelRequestDTO(employeeId, new BigDecimal("100"), new BigDecimal("10"), new BigDecimal("5.50"))
        );

        assertThat(response.litersNeeded()).isEqualByComparingTo("10.00");
        assertThat(response.totalAmount()).isEqualByComparingTo("55.00");
    }

    @Test
    @DisplayName("CLT x PJ: soma salário base + INSS patronal + FGTS + 13º/12 + férias+1/3 /12")
    void calculaComparacaoCltPj() {
        Position position = new Position();
        PositionLevel level = PositionLevel.fixed("Pleno", 1, position, new BigDecimal("3000.00"));
        CareerHistory history = new CareerHistory(
                employee, position, level, new BigDecimal("3000.00"), ContractType.CLT,
                CareerChangeReason.HIRING, LocalDate.of(2026, 1, 1), null
        );

        when(employeeRepository.findById(employeeId)).thenReturn(Optional.of(employee));
        when(careerHistoryRepository.findByEmployeeOrderByEffectiveDateDesc(employee)).thenReturn(List.of(history));

        CltPjComparisonResponseDTO response = service.compareCltPj(employeeId);

        assertThat(response.baseSalary()).isEqualByComparingTo("3000.00");
        assertThat(response.inssPatronal()).isEqualByComparingTo("600.00");
        assertThat(response.fgts()).isEqualByComparingTo("240.00");
        assertThat(response.thirteenthSalaryProvision()).isEqualByComparingTo("250.00");
        assertThat(response.vacationProvision()).isEqualByComparingTo("333.33");
        assertThat(response.totalCltCost()).isEqualByComparingTo("4423.33");
        assertThat(response.pjEquivalentValue()).isEqualByComparingTo(response.totalCltCost());
    }

    @Test
    @DisplayName("CLT x PJ lança exceção se o funcionário não tem CareerHistory")
    void cltPjSemCareerHistory() {
        when(employeeRepository.findById(employeeId)).thenReturn(Optional.of(employee));
        when(careerHistoryRepository.findByEmployeeOrderByEffectiveDateDesc(employee)).thenReturn(List.of());

        assertThrows(CareerHistoryNotFoundException.class, () -> service.compareCltPj(employeeId));
    }
}
