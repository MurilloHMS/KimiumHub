package com.proautokimium.api.Infrastructure.services.humanResources;

import com.proautokimium.api.Application.DTOs.humanResources.Calculator.*;
import com.proautokimium.api.Infrastructure.exceptions.humanResources.CareerHistoryNotFoundException;
import com.proautokimium.api.Infrastructure.exceptions.humanResources.EmployeePayrollDataMissingException;
import com.proautokimium.api.Infrastructure.repositories.EmployeeRepository;
import com.proautokimium.api.Infrastructure.repositories.humanResources.CareerHistoryRepository;
import com.proautokimium.api.domain.entities.Employee;
import com.proautokimium.api.domain.entities.humanResources.CareerHistory;
import com.proautokimium.api.domain.entities.humanResources.Company;
import com.proautokimium.api.domain.entities.humanResources.Position;
import com.proautokimium.api.domain.entities.humanResources.PositionLevel;
import com.proautokimium.api.domain.enums.humanResources.CareerChangeReason;
import com.proautokimium.api.domain.enums.humanResources.ContractType;
import com.proautokimium.api.domain.enums.humanResources.TransportType;
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

    // --- Bulk Transport Voucher ---

    @Test
    @DisplayName("Bulk VT: agrupa por empresa e calcula total = ticketPrice * commutes * workingDays")
    void calculaBulkTransportVoucher() throws Exception {
        Company proauto = buildCompany("Proauto");
        Company kimium = buildCompany("Kimium");

        Employee e1 = buildEmployeeWithTransport("Ana", "11111111111", TransportType.MUNICIPAL_BUS, 2, new BigDecimal("4.50"), proauto);
        Employee e2 = buildEmployeeWithTransport("Beto", "22222222222", TransportType.MUNICIPAL_BUS, 4, new BigDecimal("4.50"), proauto);
        Employee e3 = buildEmployeeWithTransport("Carlos", "33333333333", TransportType.MUNICIPAL_BUS, 2, new BigDecimal("5.00"), kimium);

        when(employeeRepository.findByTransportTypeAndAtivoTrue(TransportType.MUNICIPAL_BUS))
                .thenReturn(List.of(e1, e2, e3));

        List<BulkTransportVoucherResponseDTO> result = service.calculateBulkTransportVoucher(
                new BulkTransportVoucherRequestDTO(TransportType.MUNICIPAL_BUS, 22)
        );

        assertThat(result).hasSize(2);

        BulkTransportVoucherResponseDTO proautoGroup = result.stream()
                .filter(r -> "Proauto".equals(r.companyName())).findFirst().orElseThrow();
        assertThat(proautoGroup.employees()).hasSize(2);
        assertThat(proautoGroup.grandTotal()).isEqualByComparingTo("594.00");

        BulkTransportVoucherResponseDTO kimiumGroup = result.stream()
                .filter(r -> "Kimium".equals(r.companyName())).findFirst().orElseThrow();
        assertThat(kimiumGroup.employees()).hasSize(1);
        assertThat(kimiumGroup.grandTotal()).isEqualByComparingTo("220.00");
    }

    @Test
    @DisplayName("Bulk VT: retorna lista vazia se nenhum funcionário do tipo existe")
    void bulkVtSemFuncionarios() {
        when(employeeRepository.findByTransportTypeAndAtivoTrue(TransportType.INTERMUNICIPAL_BUS))
                .thenReturn(List.of());

        List<BulkTransportVoucherResponseDTO> result = service.calculateBulkTransportVoucher(
                new BulkTransportVoucherRequestDTO(TransportType.INTERMUNICIPAL_BUS, 22)
        );

        assertThat(result).isEmpty();
    }

    // --- Bulk Fuel ---

    @Test
    @DisplayName("Bulk fuel: litros = (km_diario * dias) / km_por_litro; total = litros * preço_litro")
    void calculaBulkFuel() throws Exception {
        Company proauto = buildCompany("Proauto");

        Employee e1 = buildEmployeeWithVehicle("Diego", "44444444444", new BigDecimal("12.0"), new BigDecimal("40.0"), proauto);

        when(employeeRepository.findByTransportTypeAndAtivoTrue(TransportType.VEHICLE))
                .thenReturn(List.of(e1));

        List<BulkFuelResponseDTO> result = service.calculateBulkFuel(
                new BulkFuelRequestDTO(new BigDecimal("5.50"), 22)
        );

        assertThat(result).hasSize(1);
        BulkFuelResultDTO emp = result.getFirst().employees().getFirst();
        assertThat(emp.litersNeeded()).isEqualByComparingTo("73.33");
        assertThat(emp.totalAmount()).isEqualByComparingTo("403.33");
    }

    // --- Ticket Price Adjustment ---

    @Test
    @DisplayName("Reajuste de tarifa: atualiza ticketPrice de todos os funcionários do tipo")
    void reajusteTarifa() throws Exception {
        Employee e1 = buildEmployeeWithTransport("Ana", "11111111111", TransportType.MUNICIPAL_BUS, 2, new BigDecimal("4.50"), null);
        Employee e2 = buildEmployeeWithTransport("Beto", "22222222222", TransportType.MUNICIPAL_BUS, 4, new BigDecimal("4.50"), null);

        when(employeeRepository.findByTransportTypeAndAtivoTrue(TransportType.MUNICIPAL_BUS))
                .thenReturn(List.of(e1, e2));

        TicketPriceAdjustmentResponseDTO response = service.adjustTicketPrices(
                new TicketPriceAdjustmentRequestDTO(TransportType.MUNICIPAL_BUS, new BigDecimal("5.00"))
        );

        assertThat(response.affectedCount()).isEqualTo(2);
        assertThat(e1.getTicketPrice()).isEqualByComparingTo("5.00");
        assertThat(e2.getTicketPrice()).isEqualByComparingTo("5.00");
    }

    @Test
    @DisplayName("Reajuste rejeita tipo VEHICLE")
    void reajusteRejeitaVeiculo() {
        assertThrows(IllegalArgumentException.class, () -> service.adjustTicketPrices(
                new TicketPriceAdjustmentRequestDTO(TransportType.VEHICLE, new BigDecimal("5.00"))
        ));
    }

    // --- Helpers ---

    private Company buildCompany(String name) throws Exception {
        Company c = new Company();
        c.setName(name);
        Field field = com.proautokimium.api.domain.abstractions.Entity.class.getDeclaredField("id");
        field.setAccessible(true);
        field.set(c, UUID.randomUUID());
        return c;
    }

    private Employee buildEmployeeWithTransport(String name, String doc, TransportType type,
                                                 int commutes, BigDecimal ticketPrice, Company company) throws Exception {
        Employee e = new Employee();
        e.setName(name);
        e.setDocumento(doc);
        e.setTransportType(type);
        e.setDailyCommutesCount(commutes);
        e.setTicketPrice(ticketPrice);
        e.setCompany(company);
        e.setAtivo(true);
        Field field = com.proautokimium.api.domain.abstractions.Entity.class.getDeclaredField("id");
        field.setAccessible(true);
        field.set(e, UUID.randomUUID());
        return e;
    }

    private Employee buildEmployeeWithVehicle(String name, String doc, BigDecimal kmPerLiter,
                                               BigDecimal dailyKm, Company company) throws Exception {
        Employee e = new Employee();
        e.setName(name);
        e.setDocumento(doc);
        e.setTransportType(TransportType.VEHICLE);
        e.setVehicleKmPerLiter(kmPerLiter);
        e.setDailyDistanceKm(dailyKm);
        e.setCompany(company);
        e.setAtivo(true);
        Field field = com.proautokimium.api.domain.abstractions.Entity.class.getDeclaredField("id");
        field.setAccessible(true);
        field.set(e, UUID.randomUUID());
        return e;
    }
}
