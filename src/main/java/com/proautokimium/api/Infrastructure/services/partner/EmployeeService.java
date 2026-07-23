package com.proautokimium.api.Infrastructure.services.partner;

import com.proautokimium.api.Application.DTOs.partners.CreateEmployeeRequestDTO;
import com.proautokimium.api.Application.DTOs.partners.EmployeeDTO;
import com.proautokimium.api.Application.DTOs.partners.EmployeeResponseDTO;
import com.proautokimium.api.Application.DTOs.partners.PartnerRecipientDTO;
import com.proautokimium.api.Infrastructure.exceptions.humanResources.CompanyNotFoundException;
import com.proautokimium.api.Infrastructure.exceptions.humanResources.PositionLevelNotFoundException;
import com.proautokimium.api.Infrastructure.exceptions.humanResources.PositionNotFoundException;
import com.proautokimium.api.Infrastructure.exceptions.humanResources.TeamNotFoundException;
import com.proautokimium.api.Infrastructure.repositories.EmployeeRepository;
import com.proautokimium.api.Infrastructure.repositories.humanResources.CareerHistoryRepository;
import com.proautokimium.api.Infrastructure.repositories.humanResources.CompanyRepository;
import com.proautokimium.api.Infrastructure.repositories.humanResources.PositionLevelRepository;
import com.proautokimium.api.Infrastructure.repositories.humanResources.PositionRepository;
import com.proautokimium.api.Infrastructure.repositories.humanResources.TeamRepository;
import com.proautokimium.api.Infrastructure.services.humanResources.PositionLevelSalaryResolver;
import com.proautokimium.api.domain.entities.Employee;
import com.proautokimium.api.domain.entities.humanResources.CareerHistory;
import com.proautokimium.api.domain.entities.humanResources.Company;
import com.proautokimium.api.domain.entities.humanResources.Position;
import com.proautokimium.api.domain.entities.humanResources.PositionLevel;
import com.proautokimium.api.domain.entities.humanResources.Team;
import com.proautokimium.api.domain.enums.humanResources.CareerChangeReason;
import com.proautokimium.api.domain.exceptions.partners.EmployeeNotFoundException;
import com.proautokimium.api.domain.valueObjects.Email;
import jakarta.transaction.Transactional;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;

@Service
public class EmployeeService {

    private final EmployeeRepository employeeRepository;
    private final PositionRepository positionRepository;
    private final PositionLevelRepository positionLevelRepository;
    private final CompanyRepository companyRepository;
    private final TeamRepository teamRepository;
    private final CareerHistoryRepository careerHistoryRepository;
    private final PositionLevelSalaryResolver salaryResolver;

    public EmployeeService(
            EmployeeRepository employeeRepository,
            PositionRepository positionRepository,
            PositionLevelRepository positionLevelRepository,
            CompanyRepository companyRepository,
            TeamRepository teamRepository,
            CareerHistoryRepository careerHistoryRepository,
            PositionLevelSalaryResolver salaryResolver
    ) {
        this.employeeRepository = employeeRepository;
        this.positionRepository = positionRepository;
        this.positionLevelRepository = positionLevelRepository;
        this.companyRepository = companyRepository;
        this.teamRepository = teamRepository;
        this.careerHistoryRepository = careerHistoryRepository;
        this.salaryResolver = salaryResolver;
    }

    /**
     * Cria o funcionário e já registra o primeiro snapshot de carreira (motivo HIRING),
     * na mesma transação — nunca deve existir Employee sem nenhum CareerHistory.
     */
    @Transactional
    public EmployeeResponseDTO createEmployee(CreateEmployeeRequestDTO dto) {
        Company company = companyRepository.findById(dto.companyId())
                .orElseThrow(CompanyNotFoundException::new);
        Team team = teamRepository.findById(dto.teamId())
                .orElseThrow(TeamNotFoundException::new);
        Position position = positionRepository.findById(dto.positionId())
                .orElseThrow(PositionNotFoundException::new);
        PositionLevel positionLevel = positionLevelRepository.findById(dto.positionLevelId())
                .orElseThrow(PositionLevelNotFoundException::new);

        Employee employee = new Employee();
        employee.setCodParceiro(dto.partnerCode());
        employee.setCodigoGerente(dto.managerCode());
        employee.setAtivo(dto.ativo());
        employee.setBirthday(dto.birthday());
        employee.setDocumento(dto.document());
        employee.setEmail(new Email(dto.email()));
        employee.setHierarquia(dto.hierarchy());
        employee.setName(dto.name());
        employee.setDepartment(dto.department());
        employee.setCompany(company);
        employee.setTeam(team);

        Employee savedEmployee = employeeRepository.save(employee);

        BigDecimal salary = salaryResolver.resolve(positionLevel);

        CareerHistory hiringSnapshot = new CareerHistory(
                savedEmployee,
                position,
                positionLevel,
                salary,
                dto.contractType(),
                CareerChangeReason.HIRING,
                dto.hiringDate(),
                null
        );
        careerHistoryRepository.save(hiringSnapshot);

        return toResponse(savedEmployee);
    }

    /**
     * Atualiza dados cadastrais. Cargo/nível/salário NÃO são atualizáveis por aqui —
     * mudam sempre via um novo CareerHistory (promoção, mudança de cargo, dissídio),
     * nunca por sobrescrita direta.
     */
    @Transactional
    public EmployeeResponseDTO updateEmployee(EmployeeDTO dto) {
        Employee employee = employeeRepository.findByCodParceiro(dto.partnerCode());
        if (employee == null) {
            throw new EmployeeNotFoundException();
        }

        employee.setCodigoGerente(dto.managerCode());
        employee.setAtivo(dto.ativo());
        employee.setBirthday(dto.birthday());
        employee.setDocumento(dto.document());
        employee.setEmail(new Email(dto.email()));
        employee.setHierarquia(dto.hierarchy());
        employee.setName(dto.name());
        employee.setDepartment(dto.department());

        if (dto.companyId() != null) {
            Company company = companyRepository.findById(dto.companyId())
                    .orElseThrow(CompanyNotFoundException::new);
            employee.setCompany(company);
        }
        if (dto.teamId() != null) {
            Team team = teamRepository.findById(dto.teamId())
                    .orElseThrow(TeamNotFoundException::new);
            employee.setTeam(team);
        }

        Employee saved = employeeRepository.save(employee);
        return toResponse(saved);
    }

    public List<EmployeeResponseDTO> getAllEmployes() {
        return employeeRepository.findAll().stream()
                .map(this::toResponse)
                .toList();
    }

    public List<PartnerRecipientDTO> getAllEmployesEmail() {
        return employeeRepository.findAll().stream()
                .map(m -> new PartnerRecipientDTO(
                        m.getId(),
                        m.getName(),
                        m.getEmail().getAddress(),
                        "employee"
                ))
                .toList();
    }

    private EmployeeResponseDTO toResponse(Employee employee) {
        return new EmployeeResponseDTO(
                employee.getId(),
                employee.getCodParceiro(),
                employee.getDocumento(),
                employee.getName(),
                employee.getEmail().getAddress(),
                employee.isAtivo(),
                employee.getCodigoGerente(),
                employee.getHierarquia(),
                employee.getBirthday(),
                employee.getDepartment(),
                employee.getCompany() != null ? employee.getCompany().getId() : null,
                employee.getTeam() != null ? employee.getTeam().getId() : null
        );
    }
}
