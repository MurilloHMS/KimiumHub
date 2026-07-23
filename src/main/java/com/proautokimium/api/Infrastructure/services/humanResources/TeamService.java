package com.proautokimium.api.Infrastructure.services.humanResources;

import com.proautokimium.api.Application.DTOs.humanResources.Team.CreateTeamRequestDTO;
import com.proautokimium.api.Application.DTOs.humanResources.Team.TeamResponseDTO;
import com.proautokimium.api.Infrastructure.exceptions.humanResources.DepartmentNotFoundException;
import com.proautokimium.api.Infrastructure.repositories.humanResources.DepartmentRepository;
import com.proautokimium.api.Infrastructure.repositories.humanResources.TeamRepository;
import com.proautokimium.api.domain.entities.humanResources.Department;
import com.proautokimium.api.domain.entities.humanResources.Team;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class TeamService {

    private final TeamRepository teamRepository;
    private final DepartmentRepository departmentRepository;

    public TeamService(TeamRepository teamRepository, DepartmentRepository departmentRepository) {
        this.teamRepository = teamRepository;
        this.departmentRepository = departmentRepository;
    }

    public TeamResponseDTO create(CreateTeamRequestDTO request) {
        Department department = departmentRepository.findById(request.departmentId())
                .orElseThrow(DepartmentNotFoundException::new);

        Team team = new Team(request.name(), department);
        Team saved = teamRepository.save(team);
        return toResponse(saved);
    }

    public List<TeamResponseDTO> listAll(){
        return teamRepository.findAll().stream()
                .map(this::toResponse)
                .toList();
    }

    private TeamResponseDTO toResponse(Team team){
        return new TeamResponseDTO(
                team.getId(),
                team.getName(),
                team.getDepartment()
        );
    }
}
