package com.proautokimium.api.Infrastructure.services.humanResources;

import com.proautokimium.api.Application.DTOs.humanResources.Department.DepartmentResponseDTO;
import com.proautokimium.api.Application.DTOs.humanResources.Team.CreateTeamRequestDTO;
import com.proautokimium.api.Application.DTOs.humanResources.Team.TeamResponseDTO;
import com.proautokimium.api.Infrastructure.exceptions.humanResources.CadastroEmUsoException;
import com.proautokimium.api.Infrastructure.exceptions.humanResources.DepartmentNotFoundException;
import com.proautokimium.api.Infrastructure.exceptions.humanResources.TeamNotFoundException;
import com.proautokimium.api.Infrastructure.repositories.EmployeeRepository;
import com.proautokimium.api.Infrastructure.repositories.humanResources.DepartmentRepository;
import com.proautokimium.api.Infrastructure.repositories.humanResources.TeamRepository;
import com.proautokimium.api.domain.entities.humanResources.Department;
import com.proautokimium.api.domain.entities.humanResources.Team;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Service
public class TeamService {

    private final TeamRepository teamRepository;
    private final DepartmentRepository departmentRepository;
    private final EmployeeRepository employeeRepository;

    public TeamService(
            TeamRepository teamRepository,
            DepartmentRepository departmentRepository,
            EmployeeRepository employeeRepository
    ) {
        this.teamRepository = teamRepository;
        this.departmentRepository = departmentRepository;
        this.employeeRepository = employeeRepository;
    }

    public TeamResponseDTO create(CreateTeamRequestDTO request) {
        Department department = departmentRepository.findById(request.departmentId())
                .orElseThrow(DepartmentNotFoundException::new);

        Team team = new Team(request.name(), department);
        Team saved = teamRepository.save(team);
        return toResponse(saved);
    }

    /**
     * Trocar o departamento do setor **move junto todo mundo que esta nele**:
     * desde a migracao das FKs, o departamento do funcionario e lido pelo
     * setor. Nao e efeito colateral, e o desenho — mas vale saber que a conta
     * do RH muda com esta edicao.
     */
    public TeamResponseDTO update(UUID id, CreateTeamRequestDTO request) {
        Team team = teamRepository.findById(id)
                .orElseThrow(TeamNotFoundException::new);

        Department department = departmentRepository.findById(request.departmentId())
                .orElseThrow(DepartmentNotFoundException::new);

        team.alterar(request.name(), department);

        return toResponse(teamRepository.save(team));
    }

    public void delete(UUID id) {
        Team team = teamRepository.findById(id)
                .orElseThrow(TeamNotFoundException::new);

        long funcionarios = employeeRepository.countByTeamId(id);

        if (funcionarios > 0) {
            throw new CadastroEmUsoException(
                    "Nao da para excluir: " + funcionarios
                    + (funcionarios == 1 ? " funcionario esta" : " funcionarios estao")
                    + " neste setor.");
        }

        teamRepository.delete(team);
    }

    public List<TeamResponseDTO> listAll(){
        return teamRepository.findAll().stream()
                .map(this::toResponse)
                .toList();
    }

    private TeamResponseDTO toResponse(Team team){
        Department dept = team.getDepartment();
        return new TeamResponseDTO(
                team.getId(),
                team.getName(),
                new DepartmentResponseDTO(dept.getId(), dept.getName())
        );
    }
}
