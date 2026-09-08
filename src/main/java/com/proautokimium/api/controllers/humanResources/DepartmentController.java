package com.proautokimium.api.controllers.humanResources;

import com.proautokimium.api.Application.DTOs.humanResources.Department.CreateDepartmentRequestDTO;
import com.proautokimium.api.Application.DTOs.humanResources.Department.DepartmentResponseDTO;
import com.proautokimium.api.Infrastructure.services.humanResources.DepartmentService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/hr/departments")
public class DepartmentController {
    /**
     * A leitura de referência do RH.
     *
     * Empresas, departamentos, hierarquias, times, cargos e níveis são lidos
     * pela Estrutura, por Cargos & Níveis e pelo cadastro de Funcionários —
     * os stores são compartilhados. Exigir uma tela só deixaria os combos
     * das outras vazios, sem erro nenhum na tela.
     */
    private static final String LER_ESTRUTURA_RH =
            "hasAnyAuthority('rh/organizational-structure:CONSULTAR', "
            + "'rh/career-structure:CONSULTAR', 'rh/employees:CONSULTAR')";


    private final DepartmentService departmentService;

    public DepartmentController(DepartmentService departmentService) {
        this.departmentService = departmentService;
    }

    @PostMapping
    @PreAuthorize("hasAuthority('rh/organizational-structure:INCLUIR')")
    public ResponseEntity<DepartmentResponseDTO> create(@Valid @RequestBody CreateDepartmentRequestDTO request){
        return ResponseEntity.ok(departmentService.create(request));
    }

    @GetMapping
    @PreAuthorize(LER_ESTRUTURA_RH)
    public ResponseEntity<List<DepartmentResponseDTO>> listAll() {
        return ResponseEntity.ok(departmentService.listAll());
    }


    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('rh/organizational-structure:ALTERAR')")
    public ResponseEntity<DepartmentResponseDTO> update(
            @PathVariable UUID id,
            @Valid @RequestBody CreateDepartmentRequestDTO request
    ) {
        return ResponseEntity.ok(departmentService.update(id, request));
    }

    /**
     * Recusa com 409 quando o cadastro esta em uso, e a mensagem diz por quem —
     * o front mostra essa frase inteira.
     */
    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('rh/organizational-structure:EXCLUIR')")
    public ResponseEntity<Void> delete(@PathVariable UUID id) {
        departmentService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
