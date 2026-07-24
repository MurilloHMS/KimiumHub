package com.proautokimium.api.Infrastructure.services.humanResources;

import com.proautokimium.api.Application.DTOs.humanResources.Dashboard.CompanyEmployeeCountDTO;
import com.proautokimium.api.Application.DTOs.humanResources.Dashboard.HrDashboardSummaryDTO;
import com.proautokimium.api.Application.DTOs.humanResources.Dashboard.NameCountDTO;
import com.proautokimium.api.Infrastructure.repositories.EmployeeRepository;
import com.proautokimium.api.Infrastructure.repositories.humanResources.CareerHistoryRepository;
import com.proautokimium.api.Infrastructure.repositories.humanResources.CompanyRepository;
import com.proautokimium.api.Infrastructure.repositories.humanResources.DepartmentRepository;
import com.proautokimium.api.Infrastructure.repositories.humanResources.PositionRepository;
import com.proautokimium.api.Infrastructure.repositories.humanResources.TeamRepository;
import com.proautokimium.api.domain.entities.Employee;
import com.proautokimium.api.domain.entities.humanResources.CareerHistory;
import com.proautokimium.api.domain.entities.humanResources.Company;
import com.proautokimium.api.domain.entities.humanResources.Department;
import com.proautokimium.api.domain.entities.humanResources.Position;
import com.proautokimium.api.domain.entities.humanResources.PositionLevel;
import com.proautokimium.api.domain.entities.humanResources.Team;
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
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class HrDashboardServiceTest {

    @Mock private EmployeeRepository employeeRepository;
    @Mock private CareerHistoryRepository careerHistoryRepository;
    @Mock private CompanyRepository companyRepository;
    @Mock private DepartmentRepository departmentRepository;
    @Mock private TeamRepository teamRepository;
    @Mock private PositionRepository positionRepository;

    private HrDashboardService service;

    @BeforeEach
    void setUp() {
        service = new HrDashboardService(employeeRepository, careerHistoryRepository, companyRepository, departmentRepository, teamRepository, positionRepository);
    }

    private Employee employee(String name) throws Exception {
        Employee e = new Employee();
        e.setName(name);
        Field field = com.proautokimium.api.domain.abstractions.Entity.class.getDeclaredField("id");
        field.setAccessible(true);
        field.set(e, UUID.randomUUID());
        return e;
    }

    private Company company(String name) {
        Company c = new Company();
        c.setName(name);
        return c;
    }

    private Position position(String name) {
        Position p = new Position();
        p.setName(name);
        return p;
    }

    private CareerHistory history(Employee employee, Position position, BigDecimal salary, ContractType contractType) {
        PositionLevel level = PositionLevel.fixed("Nível", 1, position, salary);
        return new CareerHistory(employee, position, level, salary, contractType, CareerChangeReason.HIRING, LocalDate.of(2026, 1, 1), null);
    }

    @Test
    @DisplayName("Agrupa funcionários por empresa, com quebra CLT/PJ")
    void agrupaPorEmpresaComQuebraCltPj() throws Exception {
        Company kimium = company("Kimium");
        Position dev = position("Desenvolvedor");

        Employee clt = employee("Fulano");
        clt.setCompany(kimium);
        Employee pj = employee("Beltrano");
        pj.setCompany(kimium);

        CareerHistory histClt = history(clt, dev, new BigDecimal("3000"), ContractType.CLT);
        CareerHistory histPj = history(pj, dev, new BigDecimal("4000"), ContractType.PJ);

        when(employeeRepository.findByAtivoTrue()).thenReturn(List.of(clt, pj));
        when(careerHistoryRepository.findLatestPerEmployee()).thenReturn(List.of(histClt, histPj));
        when(companyRepository.count()).thenReturn(1L);
        when(departmentRepository.count()).thenReturn(0L);
        when(teamRepository.count()).thenReturn(0L);
        when(positionRepository.count()).thenReturn(1L);

        HrDashboardSummaryDTO result = service.getSummary();

        assertThat(result.employeesByCompany()).hasSize(1);
        CompanyEmployeeCountDTO kimiumCount = result.employeesByCompany().get(0);
        assertThat(kimiumCount.companyName()).isEqualTo("Kimium");
        assertThat(kimiumCount.total()).isEqualTo(2);
        assertThat(kimiumCount.clt()).isEqualTo(1);
        assertThat(kimiumCount.pj()).isEqualTo(1);
    }

    @Test
    @DisplayName("Funcionário sem empresa vinculada cai em 'Sem empresa'")
    void semEmpresaCaiEmBucketPadrao() throws Exception {
        Employee semEmpresa = employee("Sem Empresa");

        when(employeeRepository.findByAtivoTrue()).thenReturn(List.of(semEmpresa));
        when(careerHistoryRepository.findLatestPerEmployee()).thenReturn(List.of());
        when(companyRepository.count()).thenReturn(0L);
        when(departmentRepository.count()).thenReturn(0L);
        when(teamRepository.count()).thenReturn(0L);
        when(positionRepository.count()).thenReturn(0L);

        HrDashboardSummaryDTO result = service.getSummary();

        assertThat(result.employeesByCompany()).hasSize(1);
        assertThat(result.employeesByCompany().get(0).companyName()).isEqualTo("Sem empresa");
        assertThat(result.employeesByCompany().get(0).clt()).isZero();
        assertThat(result.employeesByCompany().get(0).pj()).isZero();
    }

    @Test
    @DisplayName("Agrupa por cargo a partir do CareerHistory mais recente; sem histórico não entra na contagem")
    void agrupaPorCargoIgnorandoSemHistorico() throws Exception {
        Position dev = position("Desenvolvedor");
        Position rh = position("Analista de RH");

        Employee e1 = employee("Dev 1");
        Employee e2 = employee("Dev 2");
        Employee e3 = employee("RH 1");
        Employee semHistorico = employee("Sem Histórico");

        when(employeeRepository.findByAtivoTrue()).thenReturn(List.of(e1, e2, e3, semHistorico));
        when(careerHistoryRepository.findLatestPerEmployee()).thenReturn(List.of(
                history(e1, dev, new BigDecimal("3000"), ContractType.CLT),
                history(e2, dev, new BigDecimal("3200"), ContractType.CLT),
                history(e3, rh, new BigDecimal("2800"), ContractType.CLT)
        ));
        when(companyRepository.count()).thenReturn(0L);
        when(departmentRepository.count()).thenReturn(0L);
        when(teamRepository.count()).thenReturn(0L);
        when(positionRepository.count()).thenReturn(2L);

        HrDashboardSummaryDTO result = service.getSummary();

        assertThat(result.employeesByPosition()).containsExactlyInAnyOrder(
                new NameCountDTO("Desenvolvedor", 2),
                new NameCountDTO("Analista de RH", 1)
        );
    }

    @Test
    @DisplayName("Agrupa por departamento via Team->Department; sem setor cai em 'Sem departamento'")
    void agrupaPorDepartamentoViaTeam() throws Exception {
        Department ti = new Department();
        ti.setName("TI");
        Team devTeam = new Team("Desenvolvimento", ti);

        Employee comSetor = employee("Com Setor");
        comSetor.setTeam(devTeam);
        Employee semSetor = employee("Sem Setor");

        when(employeeRepository.findByAtivoTrue()).thenReturn(List.of(comSetor, semSetor));
        when(careerHistoryRepository.findLatestPerEmployee()).thenReturn(List.of());
        when(companyRepository.count()).thenReturn(0L);
        when(departmentRepository.count()).thenReturn(1L);
        when(teamRepository.count()).thenReturn(1L);
        when(positionRepository.count()).thenReturn(0L);

        HrDashboardSummaryDTO result = service.getSummary();

        assertThat(result.employeesByDepartment()).containsExactlyInAnyOrder(
                new NameCountDTO("TI", 1),
                new NameCountDTO("Sem departamento", 1)
        );
    }

    @Test
    @DisplayName("Soma o salário do CareerHistory mais recente de cada funcionário; ignora quem não tem histórico")
    void somaSalarioTotal() throws Exception {
        Position dev = position("Desenvolvedor");
        Employee e1 = employee("Dev 1");
        Employee e2 = employee("Dev 2");
        Employee semHistorico = employee("Sem Histórico");

        when(employeeRepository.findByAtivoTrue()).thenReturn(List.of(e1, e2, semHistorico));
        when(careerHistoryRepository.findLatestPerEmployee()).thenReturn(List.of(
                history(e1, dev, new BigDecimal("3000.00"), ContractType.CLT),
                history(e2, dev, new BigDecimal("4500.50"), ContractType.PJ)
        ));
        when(companyRepository.count()).thenReturn(0L);
        when(departmentRepository.count()).thenReturn(0L);
        when(teamRepository.count()).thenReturn(0L);
        when(positionRepository.count()).thenReturn(1L);

        HrDashboardSummaryDTO result = service.getSummary();

        assertThat(result.totalSalaries()).isEqualByComparingTo("7500.50");
    }

    @Test
    @DisplayName("Resumo da estrutura organizacional vem das contagens dos repositórios")
    void resumoDaEstruturaOrganizacional() {
        when(employeeRepository.findByAtivoTrue()).thenReturn(List.of());
        when(careerHistoryRepository.findLatestPerEmployee()).thenReturn(List.of());
        when(companyRepository.count()).thenReturn(3L);
        when(departmentRepository.count()).thenReturn(5L);
        when(teamRepository.count()).thenReturn(12L);
        when(positionRepository.count()).thenReturn(8L);

        HrDashboardSummaryDTO result = service.getSummary();

        assertThat(result.orgStructure().companies()).isEqualTo(3);
        assertThat(result.orgStructure().departments()).isEqualTo(5);
        assertThat(result.orgStructure().teams()).isEqualTo(12);
        assertThat(result.orgStructure().positions()).isEqualTo(8);
    }
}
