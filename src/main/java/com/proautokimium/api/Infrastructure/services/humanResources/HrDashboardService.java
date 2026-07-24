package com.proautokimium.api.Infrastructure.services.humanResources;

import com.proautokimium.api.Application.DTOs.humanResources.Dashboard.CompanyEmployeeCountDTO;
import com.proautokimium.api.Application.DTOs.humanResources.Dashboard.HrDashboardSummaryDTO;
import com.proautokimium.api.Application.DTOs.humanResources.Dashboard.NameCountDTO;
import com.proautokimium.api.Application.DTOs.humanResources.Dashboard.OrgStructureSummaryDTO;
import com.proautokimium.api.Infrastructure.repositories.EmployeeRepository;
import com.proautokimium.api.Infrastructure.repositories.humanResources.CareerHistoryRepository;
import com.proautokimium.api.Infrastructure.repositories.humanResources.CompanyRepository;
import com.proautokimium.api.Infrastructure.repositories.humanResources.DepartmentRepository;
import com.proautokimium.api.Infrastructure.repositories.humanResources.PositionRepository;
import com.proautokimium.api.Infrastructure.repositories.humanResources.TeamRepository;
import com.proautokimium.api.domain.entities.Employee;
import com.proautokimium.api.domain.entities.humanResources.CareerHistory;
import com.proautokimium.api.domain.enums.humanResources.ContractType;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class HrDashboardService {

    private static final String SEM_EMPRESA = "Sem empresa";
    private static final String SEM_DEPARTAMENTO = "Sem departamento";

    private final EmployeeRepository employeeRepository;
    private final CareerHistoryRepository careerHistoryRepository;
    private final CompanyRepository companyRepository;
    private final DepartmentRepository departmentRepository;
    private final TeamRepository teamRepository;
    private final PositionRepository positionRepository;

    public HrDashboardService(
            EmployeeRepository employeeRepository,
            CareerHistoryRepository careerHistoryRepository,
            CompanyRepository companyRepository,
            DepartmentRepository departmentRepository,
            TeamRepository teamRepository,
            PositionRepository positionRepository
    ) {
        this.employeeRepository = employeeRepository;
        this.careerHistoryRepository = careerHistoryRepository;
        this.companyRepository = companyRepository;
        this.departmentRepository = departmentRepository;
        this.teamRepository = teamRepository;
        this.positionRepository = positionRepository;
    }

    /** KPIs agregados do Painel RH: funcionários por empresa/cargo/departamento, folha total e tamanho da estrutura. */
    public HrDashboardSummaryDTO getSummary() {
        List<Employee> activeEmployees = employeeRepository.findByAtivoTrue();

        Map<UUID, CareerHistory> latestByEmployee = careerHistoryRepository.findLatestPerEmployee().stream()
                .collect(Collectors.toMap(ch -> ch.getEmployee().getId(), ch -> ch, (a, b) -> a));

        List<CompanyEmployeeCountDTO> employeesByCompany = activeEmployees.stream()
                .collect(Collectors.groupingBy(e -> e.getCompany() != null ? e.getCompany().getName() : SEM_EMPRESA))
                .entrySet().stream()
                .map(entry -> toCompanyCount(entry.getKey(), entry.getValue(), latestByEmployee))
                .sorted(Comparator.comparing(CompanyEmployeeCountDTO::companyName))
                .toList();

        List<NameCountDTO> employeesByPosition = activeEmployees.stream()
                .filter(e -> latestByEmployee.containsKey(e.getId()))
                .collect(Collectors.groupingBy(e -> latestByEmployee.get(e.getId()).getPosition().getName(), Collectors.counting()))
                .entrySet().stream()
                .map(entry -> new NameCountDTO(entry.getKey(), entry.getValue()))
                .sorted(Comparator.comparing(NameCountDTO::count).reversed())
                .toList();

        List<NameCountDTO> employeesByDepartment = activeEmployees.stream()
                .collect(Collectors.groupingBy(this::departmentName, Collectors.counting()))
                .entrySet().stream()
                .map(entry -> new NameCountDTO(entry.getKey(), entry.getValue()))
                .sorted(Comparator.comparing(NameCountDTO::count).reversed())
                .toList();

        BigDecimal totalSalaries = activeEmployees.stream()
                .map(e -> latestByEmployee.get(e.getId()))
                .filter(java.util.Objects::nonNull)
                .map(CareerHistory::getSalary)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        OrgStructureSummaryDTO orgStructure = new OrgStructureSummaryDTO(
                companyRepository.count(),
                departmentRepository.count(),
                teamRepository.count(),
                positionRepository.count()
        );

        return new HrDashboardSummaryDTO(employeesByCompany, employeesByPosition, employeesByDepartment, totalSalaries, orgStructure);
    }

    private CompanyEmployeeCountDTO toCompanyCount(String companyName, List<Employee> employees, Map<UUID, CareerHistory> latestByEmployee) {
        long clt = employees.stream().filter(e -> hasContractType(e, latestByEmployee, ContractType.CLT)).count();
        long pj = employees.stream().filter(e -> hasContractType(e, latestByEmployee, ContractType.PJ)).count();
        return new CompanyEmployeeCountDTO(companyName, employees.size(), clt, pj);
    }

    private boolean hasContractType(Employee employee, Map<UUID, CareerHistory> latestByEmployee, ContractType type) {
        CareerHistory latest = latestByEmployee.get(employee.getId());
        return latest != null && latest.getContractType() == type;
    }

    private String departmentName(Employee employee) {
        if (employee.getTeam() == null || employee.getTeam().getDepartment() == null) return SEM_DEPARTAMENTO;
        return employee.getTeam().getDepartment().getName();
    }
}
