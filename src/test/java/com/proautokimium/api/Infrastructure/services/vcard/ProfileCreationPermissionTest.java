package com.proautokimium.api.Infrastructure.services.vcard;

import com.proautokimium.api.Application.DTOs.profile.MyProfileResponseDto;
import com.proautokimium.api.Infrastructure.converters.ProfileConverter;
import com.proautokimium.api.Infrastructure.repositories.EmployeeRepository;
import com.proautokimium.api.Infrastructure.repositories.ProfileRepository;
import com.proautokimium.api.Infrastructure.repositories.UserRepository;
import com.proautokimium.api.Infrastructure.services.permission.PermissionService;
import com.proautokimium.api.Infrastructure.services.storage.ProfileImageStorageService;
import com.proautokimium.api.domain.entities.Employee;
import com.proautokimium.api.domain.entities.auth.User;
import com.proautokimium.api.domain.enums.Permission;
import com.proautokimium.api.domain.enums.UserRole;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

/**
 * Quem pode criar o cartão digital.
 *
 * **A tela de perfil é de todo mundo; criar o cartão não.** Era a role VENDEDOR
 * cravada no Java, e mudar quem podia exigia mexer em código — o que na prática
 * significava que ninguém mudava.
 *
 * Estes testes protegem as duas metades da regra, e a segunda é a que se perde
 * primeiro: quem **não** pode criar continua entrando na tela e vendo o próprio
 * perfil. Se a checagem virasse um `@PreAuthorize` na porta, essa metade sumiria
 * sem ninguém notar até alguém reclamar que o perfil não abre.
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class ProfileCreationPermissionTest {

    @Mock private ProfileRepository repository;
    @Mock private ProfileConverter converter;
    @Mock private UserRepository userRepository;
    @Mock private EmployeeRepository employeeRepository;
    @Mock private ProfileImageStorageService profileImageStorage;
    @Mock private PermissionService permissionService;

    @InjectMocks private ProfileService service;

    private static final String LOGIN = "ricardo";
    private static final String USER_ID = "u-ricardo";

    @BeforeEach
    void pessoaExiste() {
        Employee employee = new Employee();
        employee.id = UUID.randomUUID();
        employee.setName("Ricardo Souza");

        User user = new User();
        user.setId(USER_ID);
        user.setLogin(LOGIN);
        user.setEmail("ricardo@proautokimium.com.br");
        // A role continua existindo e **não decide mais nada** aqui — é
        // exatamente o que estes testes provam.
        user.setRoles(new ArrayList<>(List.of(UserRole.USER)));
        user.setEmployee(employee);

        when(userRepository.findByLoginWithEmployee(LOGIN)).thenReturn(Optional.of(user));
        when(employeeRepository.findByUsername(LOGIN)).thenReturn(Optional.of(employee));
        when(repository.findByEmployee_Id(employee.getId())).thenReturn(Optional.empty());
    }

    @Test
    @DisplayName("com perfil:INCLUIR, a tela oferece criar o cartão")
    void comPermissaoPodeCriar() {
        when(permissionService.can(USER_ID, "perfil", Permission.INCLUIR)).thenReturn(true);

        MyProfileResponseDto resposta = service.getMyProfile(LOGIN);

        assertThat(resposta.canCreateProfile()).isTrue();
    }

    /**
     * **A metade que se perde primeiro.**
     *
     * Sem a permissão, o cartão não é oferecido — mas a tela responde, com o
     * nome e o e-mail da pessoa. Um 403 aqui trancaria o perfil de quem só
     * queria ver o próprio.
     */
    @Test
    @DisplayName("sem perfil:INCLUIR, a tela abre e só não oferece criar")
    void semPermissaoAindaVeOProprioPerfil() {
        when(permissionService.can(USER_ID, "perfil", Permission.INCLUIR)).thenReturn(false);

        MyProfileResponseDto resposta = service.getMyProfile(LOGIN);

        assertThat(resposta.canCreateProfile()).isFalse();
        assertThat(resposta.employeeName()).isEqualTo("Ricardo Souza");
    }

    /**
     * A role VENDEDOR deixou de mandar nisto.
     *
     * Se este teste falhar, a regra velha voltou — e o combinado de configurar
     * pela tela em vez de por código foi desfeito sem ninguém dizer.
     */
    @Test
    @DisplayName("a role VENDEDOR sozinha não libera mais nada")
    void roleNaoDecideMais() {
        User vendedor = new User();
        vendedor.setId(USER_ID);
        vendedor.setLogin(LOGIN);
        vendedor.setRoles(new ArrayList<>(List.of(UserRole.VENDEDOR)));
        Employee employee = new Employee();
        employee.id = UUID.randomUUID();
        employee.setName("Ricardo Souza");
        vendedor.setEmployee(employee);

        when(userRepository.findByLoginWithEmployee(LOGIN)).thenReturn(Optional.of(vendedor));
        when(employeeRepository.findByUsername(LOGIN)).thenReturn(Optional.of(employee));
        when(repository.findByEmployee_Id(employee.getId())).thenReturn(Optional.empty());
        when(permissionService.can(USER_ID, "perfil", Permission.INCLUIR)).thenReturn(false);

        assertThat(service.getMyProfile(LOGIN).canCreateProfile()).isFalse();
    }
}
