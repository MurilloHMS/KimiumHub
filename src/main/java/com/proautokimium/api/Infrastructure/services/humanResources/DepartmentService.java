package com.proautokimium.api.Infrastructure.services.humanResources;

import com.proautokimium.api.Application.DTOs.humanResources.Department.CreateDepartmentRequestDTO;
import com.proautokimium.api.Application.DTOs.humanResources.Department.DepartmentResponseDTO;
import com.proautokimium.api.Infrastructure.exceptions.humanResources.CadastroEmUsoException;
import com.proautokimium.api.Infrastructure.exceptions.humanResources.DepartmentNotFoundException;
import com.proautokimium.api.Infrastructure.repositories.FuelSupplyRepository;
import com.proautokimium.api.Infrastructure.repositories.humanResources.DepartmentRepository;
import com.proautokimium.api.Infrastructure.repositories.humanResources.TeamRepository;
import com.proautokimium.api.domain.entities.humanResources.Department;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Service
public class DepartmentService {

    /**
     * O departamento onde a importacao de abastecimento joga motorista sem
     * funcionario casado. Existe no cadastro desde a V54 e o
     * `FuelSupplyController` conta com ele.
     */
    private static final String BALDE_DA_IMPORTACAO = "SEM_DEPARTAMENTO";

    private final DepartmentRepository departmentRepository;
    private final TeamRepository teamRepository;
    private final FuelSupplyRepository fuelSupplyRepository;

    public DepartmentService(
            DepartmentRepository departmentRepository,
            TeamRepository teamRepository,
            FuelSupplyRepository fuelSupplyRepository
    ) {
        this.departmentRepository = departmentRepository;
        this.teamRepository = teamRepository;
        this.fuelSupplyRepository = fuelSupplyRepository;
    }

    public DepartmentResponseDTO create(CreateDepartmentRequestDTO request){
        Department department = new Department();
        department.setName(request.name());
        Department saved = departmentRepository.save(department);
        return toResponse(saved);
    }

    public DepartmentResponseDTO update(UUID id, CreateDepartmentRequestDTO request){
        Department department = departmentRepository.findById(id)
                .orElseThrow(DepartmentNotFoundException::new);

        department.setName(request.name());
        return toResponse(departmentRepository.save(department));
    }

    /**
     * Recusa quando o departamento esta em uso, e diz por quem.
     *
     * Duas dependencias, e a segunda e nova: setores apontam para departamento
     * desde sempre, e desde a migracao das FKs **todo abastecimento** tambem.
     *
     * O SEM_DEPARTAMENTO tem guarda propria porque contagem nao o protege: num
     * banco novo ninguem aponta para ele ainda, e a exclusao passaria — a
     * proxima importacao de abastecimento e que quebraria, longe daqui.
     */
    public void delete(UUID id){
        Department department = departmentRepository.findById(id)
                .orElseThrow(DepartmentNotFoundException::new);

        if (BALDE_DA_IMPORTACAO.equalsIgnoreCase(department.getName())) {
            throw new CadastroEmUsoException(
                    "O departamento " + BALDE_DA_IMPORTACAO + " nao pode ser excluido: "
                    + "e para onde a importacao de abastecimento manda motorista "
                    + "sem funcionario correspondente.");
        }

        long setores = teamRepository.countByDepartmentId(id);
        long abastecimentos = fuelSupplyRepository.countByDepartmentId(id);

        if (setores > 0 || abastecimentos > 0) {
            throw new CadastroEmUsoException(descreverUso(setores, abastecimentos));
        }

        departmentRepository.delete(department);
    }

    private String descreverUso(long setores, long abastecimentos){
        StringBuilder texto = new StringBuilder("Nao da para excluir: ");

        if (setores > 0) {
            texto.append(setores).append(setores == 1 ? " setor usa" : " setores usam")
                 .append(" este departamento");
        }

        if (abastecimentos > 0) {
            if (setores > 0) texto.append(", e ");
            texto.append(abastecimentos)
                 .append(abastecimentos == 1 ? " abastecimento esta" : " abastecimentos estao")
                 .append(" registrado").append(abastecimentos == 1 ? "" : "s").append(" nele");
        }

        return texto.append('.').toString();
    }

    public List<DepartmentResponseDTO> listAll(){
        return departmentRepository.findAll().stream()
                .map(this::toResponse)
                .toList();
    }

    private DepartmentResponseDTO toResponse(Department department){
        return new DepartmentResponseDTO(department.getId(), department.getName());
    }
}
