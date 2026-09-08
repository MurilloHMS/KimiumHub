package com.proautokimium.api.Infrastructure.services.humanResources;

import com.proautokimium.api.Infrastructure.exceptions.humanResources.CadastroEmUsoException;
import com.proautokimium.api.Infrastructure.repositories.EmployeeRepository;
import com.proautokimium.api.Infrastructure.repositories.FuelSupplyRepository;
import com.proautokimium.api.Infrastructure.repositories.humanResources.DepartmentRepository;
import com.proautokimium.api.Infrastructure.repositories.humanResources.HierarchyRepository;
import com.proautokimium.api.Infrastructure.repositories.humanResources.TeamRepository;
import com.proautokimium.api.domain.entities.humanResources.Department;
import com.proautokimium.api.domain.entities.humanResources.Hierarchy;
import com.proautokimium.api.domain.entities.humanResources.Team;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * **Excluir cadastro em uso é recusado, e a recusa diz por quem.**
 *
 * Bloquear em vez de apagar em cascata foi decisão dele. A parte que importa
 * não é o 409 — é a frase: o front-end mostra `err.error.message` verbatim, e
 * "Conflict" não ajuda ninguém a resolver. Por isso estes testes olham o texto,
 * e não só o tipo da exceção.
 *
 * O `SEM_DEPARTAMENTO` tem um teste próprio porque contagem não o protege: num
 * banco onde ninguém aponta para ele ainda, a exclusão passaria — e quem
 * quebraria seria a próxima importação de abastecimento, longe daqui.
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class ExclusaoDeCadastroEmUsoTest {

    private static final UUID ID = UUID.randomUUID();

    // ─── Departamento ─────────────────────────────────────────────────────────

    @Nested
    @DisplayName("Departamento")
    class Departamento {

        @Mock DepartmentRepository departmentRepository;
        @Mock TeamRepository teamRepository;
        @Mock FuelSupplyRepository fuelSupplyRepository;
        @InjectMocks DepartmentService service;

        private Department departamento(String nome) {
            Department d = new Department();
            d.setName(nome);
            return d;
        }

        @Test
        @DisplayName("livre de dependências, exclui")
        void excluiQuandoLivre() {
            when(departmentRepository.findById(ID)).thenReturn(Optional.of(departamento("Industrial")));
            when(teamRepository.countByDepartmentId(ID)).thenReturn(0L);
            when(fuelSupplyRepository.countByDepartmentId(ID)).thenReturn(0L);

            service.delete(ID);

            verify(departmentRepository).delete(any(Department.class));
        }

        @Test
        @DisplayName("com setores, recusa e diz quantos")
        void recusaComSetores() {
            when(departmentRepository.findById(ID)).thenReturn(Optional.of(departamento("Industrial")));
            when(teamRepository.countByDepartmentId(ID)).thenReturn(3L);
            when(fuelSupplyRepository.countByDepartmentId(ID)).thenReturn(0L);

            assertThatThrownBy(() -> service.delete(ID))
                    .isInstanceOf(CadastroEmUsoException.class)
                    .hasMessageContaining("3")
                    .hasMessageContaining("setores");

            verify(departmentRepository, never()).delete(any(Department.class));
        }

        /**
         * A dependência nova. Antes da migração das FKs o abastecimento
         * guardava um enum e nada apontava para o cadastro.
         */
        @Test
        @DisplayName("com abastecimentos, recusa mesmo sem nenhum setor")
        void recusaComAbastecimentos() {
            when(departmentRepository.findById(ID)).thenReturn(Optional.of(departamento("Industrial")));
            when(teamRepository.countByDepartmentId(ID)).thenReturn(0L);
            when(fuelSupplyRepository.countByDepartmentId(ID)).thenReturn(12L);

            assertThatThrownBy(() -> service.delete(ID))
                    .isInstanceOf(CadastroEmUsoException.class)
                    .hasMessageContaining("12")
                    .hasMessageContaining("abastecimentos");
        }

        @Test
        @DisplayName("singular quando é um só — a frase vai para a tela")
        void concordaNoSingular() {
            when(departmentRepository.findById(ID)).thenReturn(Optional.of(departamento("Industrial")));
            when(teamRepository.countByDepartmentId(ID)).thenReturn(1L);
            when(fuelSupplyRepository.countByDepartmentId(ID)).thenReturn(0L);

            assertThatThrownBy(() -> service.delete(ID))
                    .hasMessageContaining("1 setor usa")
                    .hasMessageNotContaining("setores");
        }

        /** **O teste que a contagem sozinha não pegaria.** */
        @Test
        @DisplayName("SEM_DEPARTAMENTO não se exclui, nem sem dependência nenhuma")
        void protegeOBaldeDaImportacao() {
            when(departmentRepository.findById(ID)).thenReturn(Optional.of(departamento("SEM_DEPARTAMENTO")));
            when(teamRepository.countByDepartmentId(ID)).thenReturn(0L);
            when(fuelSupplyRepository.countByDepartmentId(ID)).thenReturn(0L);

            assertThatThrownBy(() -> service.delete(ID))
                    .isInstanceOf(CadastroEmUsoException.class)
                    .hasMessageContaining("importacao");

            verify(departmentRepository, never()).delete(any(Department.class));
        }
    }

    // ─── Setor ────────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("Setor")
    class Setor {

        @Mock TeamRepository teamRepository;
        @Mock DepartmentRepository departmentRepository;
        @Mock EmployeeRepository employeeRepository;
        @InjectMocks TeamService service;

        @Test
        @DisplayName("com funcionários, recusa e diz quantos")
        void recusaComFuncionarios() {
            when(teamRepository.findById(ID)).thenReturn(Optional.of(new Team("Produção", new Department())));
            when(employeeRepository.countByTeamId(ID)).thenReturn(7L);

            assertThatThrownBy(() -> service.delete(ID))
                    .isInstanceOf(CadastroEmUsoException.class)
                    .hasMessageContaining("7")
                    .hasMessageContaining("funcionarios");

            verify(teamRepository, never()).delete(any(Team.class));
        }

        @Test
        @DisplayName("sem funcionários, exclui")
        void excluiQuandoLivre() {
            when(teamRepository.findById(ID)).thenReturn(Optional.of(new Team("Produção", new Department())));
            when(employeeRepository.countByTeamId(ID)).thenReturn(0L);

            service.delete(ID);

            verify(teamRepository).delete(any(Team.class));
        }
    }

    // ─── Hierarquia ───────────────────────────────────────────────────────────

    @Nested
    @DisplayName("Hierarquia")
    class Hierarquia {

        @Mock HierarchyRepository hierarchyRepository;
        @Mock EmployeeRepository employeeRepository;
        @InjectMocks HierarchyService service;

        @Test
        @DisplayName("com funcionários, recusa")
        void recusaComFuncionarios() {
            when(hierarchyRepository.findById(ID)).thenReturn(Optional.of(new Hierarchy("Gerente", 2)));
            when(employeeRepository.countByHierarquiaId(ID)).thenReturn(2L);

            assertThatThrownBy(() -> service.delete(ID))
                    .isInstanceOf(CadastroEmUsoException.class)
                    .hasMessageContaining("2");

            verify(hierarchyRepository, never()).delete(any(Hierarchy.class));
        }

        @Test
        @DisplayName("sem funcionários, exclui")
        void excluiQuandoLivre() {
            when(hierarchyRepository.findById(ID)).thenReturn(Optional.of(new Hierarchy("Gerente", 2)));
            when(employeeRepository.countByHierarquiaId(ID)).thenReturn(0L);

            service.delete(ID);

            verify(hierarchyRepository).delete(any(Hierarchy.class));
        }

        @Test
        @DisplayName("alterar troca nome e ordem")
        void altera() {
            Hierarchy gerente = new Hierarchy("Gerente", 2);
            when(hierarchyRepository.findById(ID)).thenReturn(Optional.of(gerente));
            when(hierarchyRepository.save(any(Hierarchy.class))).thenAnswer(i -> i.getArgument(0));

            service.update(ID, new com.proautokimium.api.Application.DTOs.humanResources
                    .Hierarchy.CreateHierarchyRequestDTO("Gerente Sênior", 3));

            assertThat(gerente.getName()).isEqualTo("Gerente Sênior");
            assertThat(gerente.getLevelOrder()).isEqualTo(3);
        }
    }
}
