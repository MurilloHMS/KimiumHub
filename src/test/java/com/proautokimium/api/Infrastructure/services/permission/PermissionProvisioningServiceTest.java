package com.proautokimium.api.Infrastructure.services.permission;

import com.proautokimium.api.Infrastructure.repositories.permission.PermissionTemplateRepository;
import com.proautokimium.api.Infrastructure.repositories.permission.ScreenRepository;
import com.proautokimium.api.Infrastructure.repositories.permission.TemplatePermissionRepository;
import com.proautokimium.api.Infrastructure.repositories.permission.UserPermissionRepository;
import com.proautokimium.api.domain.entities.auth.User;
import com.proautokimium.api.domain.entities.permission.PermissionTemplate;
import com.proautokimium.api.domain.entities.permission.Screen;
import com.proautokimium.api.domain.entities.permission.TemplatePermission;
import com.proautokimium.api.domain.entities.permission.UserPermission;
import com.proautokimium.api.domain.enums.Permission;
import com.proautokimium.api.domain.enums.UserRole;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * O provisionamento da grade de permissões.
 *
 * O defeito que ele fecha era **invisível de três jeitos ao mesmo tempo**: a
 * pessoa entrava e não via nada, a tela de acessos desenhava normal com tudo
 * desmarcado, e salvar respondia "0 células alteradas" com sucesso. Nenhum erro,
 * em lugar nenhum, e sem conserto pela interface.
 *
 * Por isso os testes daqui afirmam sobre o que foi GRAVADO, e não sobre não ter
 * estourado: o modo de falhar desta feature é justamente não fazer nada em
 * silêncio.
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class PermissionProvisioningServiceTest {

    private static final String USER_ID = UUID.randomUUID().toString();
    private static final UUID BASE_ID = UUID.randomUUID();
    private static final UUID VENDEDOR_ID = UUID.randomUUID();

    @Mock private ScreenRepository screens;
    @Mock private UserPermissionRepository userCells;
    @Mock private PermissionTemplateRepository templates;
    @Mock private TemplatePermissionRepository templateCells;

    private PermissionProvisioningService service;

    @BeforeEach
    void setUp() {
        service = new PermissionProvisioningService(screens, userCells, templates, templateCells);

        // Duas telas, sete permissões: catorze células. O número pequeno é de
        // propósito — dá para afirmar o total exato, e 55×7 não daria.
        when(screens.findAll()).thenReturn(List.of(tela("rh/hub"), tela("estoque/programacao")));
        when(userCells.findAllOfUser(USER_ID)).thenReturn(List.of());
        when(templates.findByName(any())).thenReturn(Optional.empty());
    }

    private static Screen tela(String code) {
        Screen screen = new Screen();
        screen.setCode(code);
        return screen;
    }

    private static User pessoa(UserRole... roles) {
        User user = new User();
        user.setId(USER_ID);
        user.setRoles(new ArrayList<>(List.of(roles)));
        return user;
    }

    private static UserPermission celula(String tela, Permission permissao, boolean allowed) {
        UserPermission cell = new UserPermission();
        cell.setUserId(USER_ID);
        cell.setScreenCode(tela);
        cell.setPermission(permissao);
        cell.setAllowed(allowed);
        return cell;
    }

    private void modelo(String nome, UUID id, String tela, Permission permissao) {
        PermissionTemplate template = new PermissionTemplate();
        template.setId(id);
        template.setName(nome);
        when(templates.findByName(nome)).thenReturn(Optional.of(template));

        TemplatePermission cell = new TemplatePermission();
        cell.setTemplateId(id);
        cell.setScreenCode(tela);
        cell.setPermission(permissao);
        cell.setAllowed(true);
        when(templateCells.findByTemplateIdAndAllowedTrue(id)).thenReturn(List.of(cell));
    }

    @SuppressWarnings("unchecked")
    private List<UserPermission> gravadas() {
        ArgumentCaptor<List<UserPermission>> captor = ArgumentCaptor.forClass(List.class);
        verify(userCells).saveAll(captor.capture());
        return captor.getValue();
    }

    // ─── O caso do defeito ────────────────────────────────────────────────────

    /**
     * **O teste do bug de produção.**
     *
     * Funcionário criado por primeiro acesso ficava com zero células. A partir
     * daí `saveUserGrid` percorria uma lista vazia e devolvia "0 células
     * alteradas" — a tela dizia que gravou, e nada tinha sido gravado.
     */
    @Test
    @DisplayName("Pessoa sem nenhuma célula recebe a grade inteira")
    void grandeVaziaEhPreenchida() {
        int criadas = service.provision(pessoa(UserRole.USER));

        assertThat(criadas).isEqualTo(2 * Permission.values().length);
        assertThat(gravadas()).hasSize(14);
    }

    /**
     * **O par que garante a idempotência**, e é o que torna seguro chamar isto
     * na abertura da tela. Sem ele, abrir as permissões de alguém recriaria as
     * células toda vez — e o `saveAll` sobrescreveria os ajustes manuais.
     */
    @Test
    @DisplayName("Quem já tem a grade completa não recebe nada")
    void gradeCompletaNaoMudaNada() {
        List<UserPermission> existentes = new ArrayList<>();
        for (String tela : List.of("rh/hub", "estoque/programacao")) {
            for (Permission p : Permission.values()) existentes.add(celula(tela, p, false));
        }
        when(userCells.findAllOfUser(USER_ID)).thenReturn(existentes);

        assertThat(service.provision(pessoa(UserRole.USER))).isZero();
        verify(userCells, never()).saveAll(any());
    }

    /**
     * O caso da V87: quem foi criado entre duas migrations tem as telas antigas
     * e não as novas. É o mesmo defeito em versão parcial, e some com o mesmo
     * código — "criar na criação" não pegaria nenhum dos dois.
     */
    @Test
    @DisplayName("Grade parcial ganha só o que falta")
    void gradeParcialCompleta() {
        List<UserPermission> existentes = new ArrayList<>();
        for (Permission p : Permission.values()) existentes.add(celula("rh/hub", p, false));
        when(userCells.findAllOfUser(USER_ID)).thenReturn(existentes);

        assertThat(service.provision(pessoa(UserRole.USER))).isEqualTo(Permission.values().length);
        assertThat(gravadas())
                .extracting(UserPermission::getScreenCode)
                .containsOnly("estoque/programacao");
    }

    // ─── Quem não recebe grade ────────────────────────────────────────────────

    /**
     * **Cliente fica de fora**, como na V86 e como na consulta que lista pessoas
     * na tela de acessos. A regra mora no serviço porque os dois primeiros
     * acessos — funcionário e cliente — saem do MESMO `repository.save`: no
     * chamador, seria um `if` repetido que alguém erraria.
     */
    @Test
    @DisplayName("Cliente não recebe grade nenhuma")
    void clienteNaoRecebeGrade() {
        assertThat(service.provision(pessoa(UserRole.CLIENTE))).isZero();
        verifyNoInteractions(userCells);
    }

    /**
     * Chamado antes do `save`, o usuário ainda não tem id — e as células
     * apontariam para nada. Sair calado é melhor que gravar lixo, mas a guarda
     * existe para o dia em que alguém mover a chamada uma linha para cima.
     */
    @Test
    @DisplayName("Usuário sem id ainda não é provisionado")
    void semIdNaoProvisiona() {
        User user = new User();
        user.setRoles(List.of(UserRole.USER));

        assertThat(service.provision(user)).isZero();
        verifyNoInteractions(userCells);
    }

    // ─── O que nasce liberado ─────────────────────────────────────────────────

    /**
     * A V86 dizia "todo funcionário recebe o Base". Sem isto, o provisionamento
     * consertaria a tela de acessos e a pessoa continuaria sem ver nada — o
     * sintoma que ele relatou primeiro.
     */
    @Test
    @DisplayName("A célula do modelo Base nasce liberada")
    void baseNasceLiberado() {
        modelo("Base", BASE_ID, "rh/hub", Permission.CONSULTAR);

        service.provision(pessoa(UserRole.USER));

        assertThat(gravadas())
                .filteredOn(UserPermission::isAllowed)
                .extracting(UserPermission::getScreenCode, UserPermission::getPermission)
                .containsExactly(org.assertj.core.api.Assertions.tuple("rh/hub", Permission.CONSULTAR));
    }

    /**
     * E o modelo de cada role, SOMANDO — quem tem duas roles fica com a união,
     * que é o desenho que derrubou "um grupo por pessoa".
     */
    @Test
    @DisplayName("O modelo da role soma com o Base")
    void roleSomaComBase() {
        modelo("Base", BASE_ID, "rh/hub", Permission.CONSULTAR);
        modelo("VENDEDOR", VENDEDOR_ID, "estoque/programacao", Permission.ALTERAR);

        service.provision(pessoa(UserRole.USER, UserRole.VENDEDOR));

        assertThat(gravadas())
                .filteredOn(UserPermission::isAllowed)
                .extracting(UserPermission::getScreenCode)
                .containsExactlyInAnyOrder("rh/hub", "estoque/programacao");
    }

    /** Role sem modelo correspondente não pode derrubar a criação do usuário. */
    @Test
    @DisplayName("Role sem modelo não estoura")
    void roleSemModeloNaoEstoura() {
        assertThat(service.provision(pessoa(UserRole.PARCEIRO)))
                .isEqualTo(2 * Permission.values().length);
    }

    /**
     * **Só o que nasce agora é liberado.**
     *
     * Numa grade parcial, reaplicar o modelo por cima das células que já
     * existiam desfaria qualquer ajuste manual — e o método deixaria de ser
     * seguro de chamar na abertura da tela, que é a razão de ele existir onde
     * está.
     */
    @Test
    @DisplayName("Célula que já existia não é tocada pelo modelo")
    void naoReaplicaModeloNoQueJaExistia() {
        when(userCells.findAllOfUser(USER_ID))
                .thenReturn(List.of(celula("rh/hub", Permission.CONSULTAR, false)));
        modelo("Base", BASE_ID, "rh/hub", Permission.CONSULTAR);

        service.provision(pessoa(UserRole.USER));

        assertThat(gravadas())
                .noneMatch(cell -> cell.getScreenCode().equals("rh/hub")
                        && cell.getPermission() == Permission.CONSULTAR);
    }

    // ─── A transação ──────────────────────────────────────────────────────────

    /**
     * **A guarda do defeito que foi para produção em 01/09.**
     *
     * O método nasceu {@code REQUIRES_NEW}, para escapar do {@code readOnly} do
     * `userGrid`, e derrubou a criação de usuário com violação de chave
     * estrangeira: o `signInFirstAccess` é `@Transactional` e salva o usuário
     * dentro da transação dele; uma transação NOVA não enxerga linha ainda não
     * commitada por outra, e o `user_id` não existia do lado de cá.
     *
     * <p>Este teste é feio de propósito — ele lê uma anotação em vez de exercer
     * comportamento. Existe porque **nada mais alcança isto**:
     *
     * <ul>
     *   <li>os outros testes daqui usam mock, e mock não tem transação;</li>
     *   <li>um teste de integração em H2 também não pegaria: a chave
     *       estrangeira vive na migration V86, e o H2 monta o schema pelas
     *       anotações — {@code UserPermission.userId} é uma String comum, não
     *       um relacionamento, então em H2 não existe FK nenhuma para violar.</li>
     * </ul>
     *
     * <p>Ou seja: só o Postgres de verdade reprova, e ele só é consultado depois
     * do deploy. Uma asserção sobre a anotação é o que sobra.
     */
    @Test
    @DisplayName("provision se junta à transação de quem chama, nunca abre uma nova")
    void provisionNaoAbreTransacaoPropria() throws NoSuchMethodException {
        Transactional anotacao = PermissionProvisioningService.class
                .getMethod("provision", com.proautokimium.api.domain.entities.auth.User.class)
                .getAnnotation(Transactional.class);

        assertThat(anotacao).isNotNull();
        assertThat(anotacao.propagation()).isEqualTo(Propagation.REQUIRED);
    }
}
