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
import com.proautokimium.api.domain.enums.humanResources.ContractType;
import com.proautokimium.api.domain.exceptions.partners.EmployeeNotFoundException;
import com.proautokimium.api.domain.valueObjects.Email;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.lang.reflect.Field;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class EmployeeServiceTest {

    @Mock private EmployeeRepository employeeRepository;
    @Mock private PositionRepository positionRepository;
    @Mock private PositionLevelRepository positionLevelRepository;
    @Mock private CompanyRepository companyRepository;
    @Mock private TeamRepository teamRepository;
    @Mock private CareerHistoryRepository careerHistoryRepository;
    @Mock private PositionLevelSalaryResolver salaryResolver;

    private EmployeeService employeeService;

    private UUID companyId;
    private UUID teamId;
    private UUID positionId;
    private UUID positionLevelId;
    private Company company;
    private Team team;
    private Position position;
    private PositionLevel positionLevel;
    private CreateEmployeeRequestDTO createDto;

    @BeforeEach
    void setUp() throws Exception {
        employeeService = new EmployeeService(
                employeeRepository, positionRepository, positionLevelRepository,
                companyRepository, teamRepository, careerHistoryRepository, salaryResolver
        );

        companyId = UUID.randomUUID();
        teamId = UUID.randomUUID();
        positionId = UUID.randomUUID();
        positionLevelId = UUID.randomUUID();

        company = new Company();
        setId(company, companyId);
        team = new Team();
        setId(team, teamId);
        position = new Position();
        setId(position, positionId);
        positionLevel = PositionLevel.fixed("Júnior", 1, position, new BigDecimal("1690.00"));
        setId(positionLevel, positionLevelId);

        createDto = new CreateEmployeeRequestDTO(
                "EMP001", "12345678900", "Funcionario Teste", "func@teste.com", true, "MGR001",
                null, LocalDate.of(1990, 1, 1), null,
                companyId, teamId, positionId, positionLevelId,
                ContractType.CLT, LocalDate.of(2026, 7, 23)
        );
    }

    private void setId(com.proautokimium.api.domain.abstractions.Entity entity, UUID id) throws Exception {
        Field field = com.proautokimium.api.domain.abstractions.Entity.class.getDeclaredField("id");
        field.setAccessible(true);
        field.set(entity, id);
    }

    @Test
    @DisplayName("Deve criar funcionário e o primeiro CareerHistory (HIRING) na mesma operação")
    void deveCriarFuncionarioComPrimeiroCareerHistory() {
        when(companyRepository.findById(companyId)).thenReturn(Optional.of(company));
        when(teamRepository.findById(teamId)).thenReturn(Optional.of(team));
        when(positionRepository.findById(positionId)).thenReturn(Optional.of(position));
        when(positionLevelRepository.findById(positionLevelId)).thenReturn(Optional.of(positionLevel));
        when(salaryResolver.resolve(positionLevel)).thenReturn(new BigDecimal("1690.00"));
        when(employeeRepository.save(any(Employee.class))).thenAnswer(invocation -> invocation.getArgument(0));

        EmployeeResponseDTO response = employeeService.createEmployee(createDto);

        assertThat(response.partnerCode()).isEqualTo("EMP001");
        assertThat(response.companyId()).isEqualTo(companyId);
        assertThat(response.teamId()).isEqualTo(teamId);

        ArgumentCaptor<CareerHistory> historyCaptor = ArgumentCaptor.forClass(CareerHistory.class);
        verify(careerHistoryRepository).save(historyCaptor.capture());

        CareerHistory savedHistory = historyCaptor.getValue();
        assertThat(savedHistory.getReason().name()).isEqualTo("HIRING");
        assertThat(savedHistory.getSalary()).isEqualByComparingTo("1690.00");
        assertThat(savedHistory.getContractType()).isEqualTo(ContractType.CLT);
    }

    @Test
    @DisplayName("Não deve criar funcionário nem CareerHistory se a empresa não existir")
    void deveLancarExcecaoSeEmpresaNaoExistir() {
        when(companyRepository.findById(companyId)).thenReturn(Optional.empty());

        assertThrows(CompanyNotFoundException.class, () -> employeeService.createEmployee(createDto));

        verify(employeeRepository, never()).save(any());
        verify(careerHistoryRepository, never()).save(any());
    }

    @Test
    @DisplayName("Não deve criar funcionário se o setor não existir")
    void deveLancarExcecaoSeSetorNaoExistir() {
        when(companyRepository.findById(companyId)).thenReturn(Optional.of(company));
        when(teamRepository.findById(teamId)).thenReturn(Optional.empty());

        assertThrows(TeamNotFoundException.class, () -> employeeService.createEmployee(createDto));

        verify(employeeRepository, never()).save(any());
    }

    @Test
    @DisplayName("Não deve criar funcionário se o cargo não existir")
    void deveLancarExcecaoSeCargoNaoExistir() {
        when(companyRepository.findById(companyId)).thenReturn(Optional.of(company));
        when(teamRepository.findById(teamId)).thenReturn(Optional.of(team));
        when(positionRepository.findById(positionId)).thenReturn(Optional.empty());

        assertThrows(PositionNotFoundException.class, () -> employeeService.createEmployee(createDto));
    }

    @Test
    @DisplayName("Não deve criar funcionário se o nível do cargo não existir")
    void deveLancarExcecaoSeNivelNaoExistir() {
        when(companyRepository.findById(companyId)).thenReturn(Optional.of(company));
        when(teamRepository.findById(teamId)).thenReturn(Optional.of(team));
        when(positionRepository.findById(positionId)).thenReturn(Optional.of(position));
        when(positionLevelRepository.findById(positionLevelId)).thenReturn(Optional.empty());

        assertThrows(PositionLevelNotFoundException.class, () -> employeeService.createEmployee(createDto));
    }

    @Test
    @DisplayName("Deve atualizar funcionário existente")
    void deveAtualizarFuncionarioComSucesso() throws Exception {
        Employee existing = new Employee();
        existing.setEmail(new Email("antigo@teste.com"));

        EmployeeDTO updateDto = new EmployeeDTO(
                "EMP001", "12345678900", "Funcionario Atualizado", "novo@teste.com", true,
                "MGR001", null, LocalDate.of(1990, 1, 1), null, null, null
        );

        when(employeeRepository.findByCodParceiro("EMP001")).thenReturn(existing);
        when(employeeRepository.save(existing)).thenReturn(existing);

        EmployeeResponseDTO response = employeeService.updateEmployee(updateDto);

        assertThat(response.name()).isEqualTo("Funcionario Atualizado");
        assertThat(response.email()).isEqualTo("novo@teste.com");
    }

    @Test
    @DisplayName("Deve lançar EmployeeNotFoundException ao atualizar funcionário inexistente")
    void deveLancarExcecaoAoAtualizarFuncionarioInexistente() {
        EmployeeDTO updateDto = new EmployeeDTO(
                "EMP001", "12345678900", "Teste", "func@teste.com", true,
                "MGR001", null, null, null, null, null
        );

        when(employeeRepository.findByCodParceiro("EMP001")).thenReturn(null);

        assertThrows(EmployeeNotFoundException.class, () -> employeeService.updateEmployee(updateDto));
        verify(employeeRepository, never()).save(any());
    }

    @Test
    @DisplayName("Deve retornar lista de funcionários")
    void deveRetornarListaDeFuncionarios() {
        Employee employee = new Employee();
        employee.setCodParceiro("EMP001");
        employee.setEmail(new Email("func@teste.com"));
        when(employeeRepository.findAll()).thenReturn(List.of(employee));

        List<EmployeeResponseDTO> response = employeeService.getAllEmployes();

        assertThat(response).hasSize(1);
        assertThat(response.get(0).partnerCode()).isEqualTo("EMP001");
    }

    @Test
    @DisplayName("Deve retornar lista de emails de funcionários")
    void deveRetornarListaDeEmailsDeFuncionarios() {
        Employee employee = new Employee();
        employee.setName("Funcionario Teste");
        employee.setEmail(new Email("func@teste.com"));
        when(employeeRepository.findAll()).thenReturn(List.of(employee));

        List<PartnerRecipientDTO> response = employeeService.getAllEmployesEmail();

        assertThat(response).hasSize(1);
        assertThat(response.get(0).email()).isEqualTo("func@teste.com");
    }
}
