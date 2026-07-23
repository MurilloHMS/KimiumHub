package com.proautokimium.api.controllers.humanResources;

import com.proautokimium.api.Application.DTOs.humanResources.Team.CreateTeamRequestDTO;
import com.proautokimium.api.Application.DTOs.humanResources.Team.TeamResponseDTO;
import com.proautokimium.api.Infrastructure.services.humanResources.TeamService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/hr/teams")
public class TeamController {

    private final TeamService teamService;

    public TeamController(TeamService teamService) {
        this.teamService = teamService;
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'RH')")
    public ResponseEntity<TeamResponseDTO> create(@Valid @RequestBody CreateTeamRequestDTO request) {
        return ResponseEntity.ok(teamService.create(request));
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'RH')")
    public ResponseEntity<List<TeamResponseDTO>> listAll() {
        return ResponseEntity.ok(teamService.listAll());
    }
}
