package com.proautokimium.api.Infrastructure.services.humanResources;

import com.proautokimium.api.Application.DTOs.humanResources.Calculator.*;
import com.proautokimium.api.Infrastructure.exceptions.humanResources.CareerHistoryNotFoundException;
import com.proautokimium.api.Infrastructure.exceptions.humanResources.EmployeePayrollDataMissingException;
import com.proautokimium.api.Infrastructure.repositories.EmployeeRepository;
import com.proautokimium.api.Infrastructure.repositories.humanResources.CareerHistoryRepository;
import com.proautokimium.api.domain.entities.Employee;
import com.proautokimium.api.domain.entities.humanResources.CareerHistory;
import com.proautokimium.api.domain.enums.humanResources.TransportType;
import com.proautokimium.api.domain.exceptions.partners.EmployeeNotFoundException;
import jakarta.transaction.Transactional;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Calculadoras do RH. São cálculos puros (sem persistência) — o valor de referência
 * (preço da condução, valor da refeição, preço do litro etc.) sempre entra como
 * parâmetro, nunca fica salvo, porque muda com frequência.
 */
@Service
public class PayrollCalculatorService {

    private static final BigDecimal INSS_PATRONAL_RATE = new BigDecimal("0.20");
    private static final BigDecimal FGTS_RATE = new BigDecimal("0.08");
    private static final BigDecimal FOUR = new BigDecimal("4");
    private static final BigDecimal THREE = new BigDecimal("3");
    private static final BigDecimal TWELVE = new BigDecimal("12");

    private final EmployeeRepository employeeRepository;
    private final CareerHistoryRepository careerHistoryRepository;

    public PayrollCalculatorService(EmployeeRepository employeeRepository, CareerHistoryRepository careerHistoryRepository) {
        this.employeeRepository = employeeRepository;
        this.careerHistoryRepository = careerHistoryRepository;
    }

    public TransportationVoucherResponseDTO calculateTransportationVoucher(TransportationVoucherRequestDTO request) {
        Employee employee = findEmployee(request.employeeId());
        Integer commutes = employee.getDailyCommutesCount();
        if (commutes == null) {
            throw new EmployeePayrollDataMissingException("Funcionário não tem quantidade de conduções cadastrada");
        }

        BigDecimal total = request.fareValue()
                .multiply(BigDecimal.valueOf(commutes))
                .multiply(BigDecimal.valueOf(request.workingDays()))
                .setScale(2, RoundingMode.HALF_UP);

        return new TransportationVoucherResponseDTO(
                employee.getId(), employee.getName(), commutes, request.fareValue(), request.workingDays(), total
        );
    }

    public MealVoucherResponseDTO calculateMealVoucher(MealVoucherRequestDTO request) {
        Employee employee = findEmployee(request.employeeId());
        Integer meals = employee.getDailyMealsCount();
        if (meals == null) {
            throw new EmployeePayrollDataMissingException("Funcionário não tem quantidade de refeições cadastrada");
        }

        BigDecimal total = request.mealValue()
                .multiply(BigDecimal.valueOf(meals))
                .multiply(BigDecimal.valueOf(request.workingDays()))
                .setScale(2, RoundingMode.HALF_UP);

        return new MealVoucherResponseDTO(
                employee.getId(), employee.getName(), meals, request.mealValue(), request.workingDays(), total
        );
    }

    public FuelResponseDTO calculateFuel(FuelRequestDTO request) {
        Employee employee = findEmployee(request.employeeId());

        BigDecimal liters = request.distanceKm()
                .divide(request.vehicleConsumptionKmPerLiter(), 4, RoundingMode.HALF_UP);
        BigDecimal total = liters.multiply(request.literPrice()).setScale(2, RoundingMode.HALF_UP);

        return new FuelResponseDTO(
                employee.getId(), employee.getName(), request.distanceKm(),
                liters.setScale(2, RoundingMode.HALF_UP), request.literPrice(), total
        );
    }

    /**
     * Simulação: pega o salário atual do funcionário (último CareerHistory, seja
     * qual for o contractType hoje) e calcula quanto custaria como CLT de verdade,
     * incluindo encargos. Percentuais são os padrões de mercado — ponto de partida
     * configurável, não os números fiscais exatos da Proauto.
     */
    public CltPjComparisonResponseDTO compareCltPj(UUID employeeId) {
        Employee employee = findEmployee(employeeId);

        CareerHistory latest = careerHistoryRepository.findByEmployeeOrderByEffectiveDateDesc(employee)
                .stream()
                .findFirst()
                .orElseThrow(CareerHistoryNotFoundException::new);

        BigDecimal baseSalary = latest.getSalary();

        BigDecimal inssPatronal = baseSalary.multiply(INSS_PATRONAL_RATE).setScale(2, RoundingMode.HALF_UP);
        BigDecimal fgts = baseSalary.multiply(FGTS_RATE).setScale(2, RoundingMode.HALF_UP);
        BigDecimal thirteenthSalaryProvision = baseSalary.divide(TWELVE, 2, RoundingMode.HALF_UP);
        BigDecimal vacationProvision = baseSalary.multiply(FOUR).divide(THREE, 10, RoundingMode.HALF_UP)
                .divide(TWELVE, 2, RoundingMode.HALF_UP);

        BigDecimal totalCltCost = baseSalary.add(inssPatronal).add(fgts)
                .add(thirteenthSalaryProvision).add(vacationProvision);

        return new CltPjComparisonResponseDTO(
                employee.getId(), employee.getName(), baseSalary,
                inssPatronal, fgts, thirteenthSalaryProvision, vacationProvision,
                totalCltCost, totalCltCost
        );
    }

    public List<BulkTransportVoucherResponseDTO> calculateBulkTransportVoucher(BulkTransportVoucherRequestDTO request) {
        List<Employee> employees = employeeRepository.findByTransportTypeAndAtivoTrue(request.transportType())
                .stream()
                .filter(e -> e.getDailyCommutesCount() != null && e.getTicketPrice() != null)
                .toList();

        Map<String, List<Employee>> byCompany = employees.stream()
                .collect(Collectors.groupingBy(
                        e -> e.getCompany() != null ? e.getCompany().getName() : "Sem empresa",
                        Collectors.toList()
                ));

        return byCompany.entrySet().stream().map(entry -> {
            List<BulkTransportVoucherResultDTO> results = entry.getValue().stream().map(e -> {
                BigDecimal total = e.getTicketPrice()
                        .multiply(BigDecimal.valueOf(e.getDailyCommutesCount()))
                        .multiply(BigDecimal.valueOf(request.workingDays()))
                        .setScale(2, RoundingMode.HALF_UP);

                return new BulkTransportVoucherResultDTO(
                        e.getId(), e.getName(), e.getDocumento(),
                        e.getDailyCommutesCount(), e.getTicketPrice(),
                        request.workingDays(), total
                );
            }).toList();

            BigDecimal grandTotal = results.stream()
                    .map(BulkTransportVoucherResultDTO::totalAmount)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);

            UUID companyId = entry.getValue().getFirst().getCompany() != null
                    ? entry.getValue().getFirst().getCompany().getId() : null;

            return new BulkTransportVoucherResponseDTO(entry.getKey(), companyId, results, grandTotal);
        }).toList();
    }

    public List<BulkFuelResponseDTO> calculateBulkFuel(BulkFuelRequestDTO request) {
        List<Employee> employees = employeeRepository.findByTransportTypeAndAtivoTrue(TransportType.VEHICLE)
                .stream()
                .filter(e -> e.getVehicleKmPerLiter() != null && e.getDailyDistanceKm() != null)
                .toList();

        Map<String, List<Employee>> byCompany = employees.stream()
                .collect(Collectors.groupingBy(
                        e -> e.getCompany() != null ? e.getCompany().getName() : "Sem empresa",
                        Collectors.toList()
                ));

        return byCompany.entrySet().stream().map(entry -> {
            List<BulkFuelResultDTO> results = entry.getValue().stream().map(e -> {
                BigDecimal totalKm = e.getDailyDistanceKm()
                        .multiply(BigDecimal.valueOf(request.workingDays()));
                BigDecimal liters = totalKm
                        .divide(e.getVehicleKmPerLiter(), 4, RoundingMode.HALF_UP);
                BigDecimal total = liters.multiply(request.fuelPricePerLiter())
                        .setScale(2, RoundingMode.HALF_UP);

                return new BulkFuelResultDTO(
                        e.getId(), e.getName(), e.getDocumento(),
                        e.getDailyDistanceKm(), e.getVehicleKmPerLiter(),
                        liters.setScale(2, RoundingMode.HALF_UP), total
                );
            }).toList();

            BigDecimal grandTotal = results.stream()
                    .map(BulkFuelResultDTO::totalAmount)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);

            UUID companyId = entry.getValue().getFirst().getCompany() != null
                    ? entry.getValue().getFirst().getCompany().getId() : null;

            return new BulkFuelResponseDTO(entry.getKey(), companyId, results, grandTotal);
        }).toList();
    }

    @Transactional
    public TicketPriceAdjustmentResponseDTO adjustTicketPrices(TicketPriceAdjustmentRequestDTO request) {
        if (request.transportType() == TransportType.VEHICLE) {
            throw new IllegalArgumentException("Reajuste de tarifa só se aplica a transporte público");
        }

        List<Employee> employees = employeeRepository.findByTransportTypeAndAtivoTrue(request.transportType());
        employees.forEach(e -> e.setTicketPrice(request.newTicketPrice()));
        employeeRepository.saveAll(employees);

        return new TicketPriceAdjustmentResponseDTO(
                employees.size(), request.transportType(), request.newTicketPrice()
        );
    }

    private Employee findEmployee(UUID employeeId) {
        return employeeRepository.findById(employeeId).orElseThrow(EmployeeNotFoundException::new);
    }
}
