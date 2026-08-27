package com.proautokimium.api.Infrastructure.services.permission;

import com.proautokimium.api.Application.DTOs.permission.PermissionDTOs.*;
import com.proautokimium.api.Infrastructure.repositories.UserRepository;
import com.proautokimium.api.Infrastructure.repositories.permission.*;
import com.proautokimium.api.domain.entities.auth.User;
import com.proautokimium.api.domain.entities.permission.*;
import com.proautokimium.api.domain.enums.ApplyMode;
import com.proautokimium.api.domain.enums.Permission;
import com.proautokimium.api.domain.enums.UserRole;
import com.proautokimium.api.domain.exceptions.permission.ClientHasNoScreenPermissionsException;
import com.proautokimium.api.domain.exceptions.permission.PermissionTemplateNameInUseException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

/**
 * O carimbo, e o que ele faz com quem já estava configurado.
 *
 * Estes testes existem porque a diferença entre SOMAR e SUBSTITUIR **não dá
 * erro quando está errada**: as duas gravam, as duas respondem 200, e a pessoa
 * só descobre no dia seguinte que perdeu o acesso a uma tela. É o tipo de
 * defeito que só um teste pega antes.
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class PermissionAdminServiceTest {

    @Mock private ScreenRepository screens;
    @Mock private PermissionTemplateRepository templates;
    @Mock private TemplatePermissionRepository templateCells;
    @Mock private UserPermissionRepository userCells;
    @Mock private UserTemplateRepository stamps;
    @Mock private UserRepository users;
    @Mock private PermissionService permissions;

    @InjectMocks private PermissionAdminService service;

    private static final String WESLLEY = "u-weslley";
    private static final String RICARDO = "u-ricardo";
    private static final UUID ESTOQUE = UUID.randomUUID();

    private static final String MOVIMENTACOES = "stock/movements";
    private static final String PRODUTOS = "stock/products";

    @BeforeEach
    void funcionariosExistem() {
        when(users.findById(WESLLEY)).thenReturn(Optional.of(funcionario(WESLLEY)));
        when(users.findById(RICARDO)).thenReturn(Optional.of(funcionario(RICARDO)));
        when(templates.findById(ESTOQUE)).thenReturn(Optional.of(modelo(ESTOQUE, "ESTOQUE")));
    }

    // ─── Fixtures ────────────────────────────────────────────────────────────

    private static User funcionario(String id) {
        User user = new User();
        user.setId(id);
        user.setLogin(id);
        user.setRoles(new ArrayList<>(List.of(UserRole.USER)));
        return user;
    }

    private static User cliente(String id) {
        User user = funcionario(id);
        user.setRoles(new ArrayList<>(List.of(UserRole.CLIENTE)));
        return user;
    }

    private static PermissionTemplate modelo(UUID id, String nome) {
        PermissionTemplate template = new PermissionTemplate();
        template.setId(id);
        template.setName(nome);
        return template;
    }

    private static UserPermission celula(String userId, String tela, Permission permissao, boolean ligada) {
        UserPermission cell = new UserPermission();
        cell.setUserId(userId);
        cell.setScreenCode(tela);
        cell.setPermission(permissao);
        cell.setAllowed(ligada);
        return cell;
    }

    private static TemplatePermission celulaDoModelo(UUID templateId, String tela, Permission permissao) {
        TemplatePermission cell = new TemplatePermission();
        cell.setTemplateId(templateId);
        cell.setScreenCode(tela);
        cell.setPermission(permissao);
        cell.setAllowed(true);
        return cell;
    }

    /** O que o modelo ESTOQUE libera: consultar produtos, e só. */
    private void modeloEstoqueLibera(String tela, Permission permissao) {
        when(templateCells.findByTemplateIdAndAllowedTrue(ESTOQUE))
                .thenReturn(List.of(celulaDoModelo(ESTOQUE, tela, permissao)));
    }

    private static boolean ligada(List<UserPermission> cells, String tela, Permission permissao) {
        return cells.stream()
                .filter(c -> c.getScreenCode().equals(tela) && c.getPermission() == permissao)
                .findFirst().orElseThrow().isAllowed();
    }

    // ─── SOMAR e SUBSTITUIR ──────────────────────────────────────────────────

    /**
     * **O caso que desenhou a feature inteira.**
     *
     * O vendedor que mexe com telas do estoque recebe VENDAS e depois ESTOQUE.
     * Se o segundo carimbo apagasse o primeiro, ele perderia as telas de vendas
     * no momento em que ganhasse as de estoque — e a única saída seria um
     * modelo "Vendas + Estoque", que é exatamente a explosão de grupos que este
     * desenho evita.
     */
    @Test
    @DisplayName("SOMAR liga o que o modelo dá e não desliga nada")
    void somarNaoApaga() {
        List<UserPermission> grade = new ArrayList<>(List.of(
                celula(RICARDO, MOVIMENTACOES, Permission.CONSULTAR, true),
                celula(RICARDO, PRODUTOS, Permission.CONSULTAR, false)));
        when(userCells.findByUserIdIn(List.of(RICARDO))).thenReturn(grade);
        modeloEstoqueLibera(PRODUTOS, Permission.CONSULTAR);

        ApplyResultDTO resultado = service.apply(ESTOQUE,
                new ApplyTemplateDTO(List.of(RICARDO), ApplyMode.SOMAR), "murillo");

        assertThat(ligada(grade, MOVIMENTACOES, Permission.CONSULTAR))
                .as("o que já estava ligado continua ligado")
                .isTrue();
        assertThat(ligada(grade, PRODUTOS, Permission.CONSULTAR)).isTrue();
        assertThat(resultado.cellsChanged()).isEqualTo(1);
    }

    /**
     * O reaplicar, e a razão de ele avisar antes.
     *
     * SUBSTITUIR é o único caminho pelo qual um ajuste individual se perde. Se
     * ele parasse de desligar, "reaplicar" viraria um SOMAR silencioso e a tela
     * estaria mentindo no aviso que dá.
     */
    @Test
    @DisplayName("SUBSTITUIR desliga o que o modelo não tem")
    void substituirApaga() {
        List<UserPermission> grade = new ArrayList<>(List.of(
                celula(RICARDO, MOVIMENTACOES, Permission.CONSULTAR, true),
                celula(RICARDO, PRODUTOS, Permission.CONSULTAR, false)));
        when(userCells.findByUserIdIn(List.of(RICARDO))).thenReturn(grade);
        modeloEstoqueLibera(PRODUTOS, Permission.CONSULTAR);

        service.apply(ESTOQUE, new ApplyTemplateDTO(List.of(RICARDO), ApplyMode.SUBSTITUIR), "murillo");

        assertThat(ligada(grade, MOVIMENTACOES, Permission.CONSULTAR)).isFalse();
        assertThat(ligada(grade, PRODUTOS, Permission.CONSULTAR)).isTrue();
    }

    /** Modo ausente é SOMAR — o corpo velho de uma tela antiga não pode apagar acesso. */
    @Test
    @DisplayName("sem modo no corpo, soma")
    void semModoSoma() {
        List<UserPermission> grade = new ArrayList<>(List.of(
                celula(RICARDO, MOVIMENTACOES, Permission.CONSULTAR, true)));
        when(userCells.findByUserIdIn(List.of(RICARDO))).thenReturn(grade);
        modeloEstoqueLibera(PRODUTOS, Permission.CONSULTAR);

        service.apply(ESTOQUE, new ApplyTemplateDTO(List.of(RICARDO), null), "murillo");

        assertThat(ligada(grade, MOVIMENTACOES, Permission.CONSULTAR)).isTrue();
    }

    @Test
    @DisplayName("o carimbo fica registrado, com quem aplicou e em que modo")
    void carimboFicaRegistrado() {
        when(userCells.findByUserIdIn(List.of(WESLLEY))).thenReturn(new ArrayList<>());
        modeloEstoqueLibera(PRODUTOS, Permission.CONSULTAR);

        service.apply(ESTOQUE, new ApplyTemplateDTO(List.of(WESLLEY), ApplyMode.SUBSTITUIR), "murillo");

        ArgumentCaptor<UserTemplate> carimbo = ArgumentCaptor.forClass(UserTemplate.class);
        verify(stamps).save(carimbo.capture());
        assertThat(carimbo.getValue().getUserId()).isEqualTo(WESLLEY);
        assertThat(carimbo.getValue().getTemplateId()).isEqualTo(ESTOQUE);
        assertThat(carimbo.getValue().getAppliedBy()).isEqualTo("murillo");
        assertThat(carimbo.getValue().getMode()).isEqualTo(ApplyMode.SUBSTITUIR);
    }

    // ─── O cache ─────────────────────────────────────────────────────────────

    /**
     * **O `forget` é o que decide se a feature funciona.**
     *
     * Sem ele o defeito é o mais chato desta feature: a permissão muda no
     * banco, a tela de configuração mostra o valor novo, e a API continua
     * recusando até a pessoa sair e entrar. Parece bug de front, some quando
     * alguém vai investigar, e volta no dia seguinte.
     */
    @Test
    @DisplayName("carimbar esquece o cache de cada pessoa alcançada")
    void carimbarEsqueceOCacheDeTodos() {
        when(userCells.findByUserIdIn(any())).thenReturn(new ArrayList<>());
        modeloEstoqueLibera(PRODUTOS, Permission.CONSULTAR);

        service.apply(ESTOQUE,
                new ApplyTemplateDTO(List.of(WESLLEY, RICARDO), ApplyMode.SOMAR), "murillo");

        verify(permissions).forget(WESLLEY);
        verify(permissions).forget(RICARDO);
    }

    @Test
    @DisplayName("gravar a grade de uma pessoa esquece o cache dela")
    void gravarPessoaEsqueceOCache() {
        when(userCells.findAllOfUser(WESLLEY)).thenReturn(new ArrayList<>(List.of(
                celula(WESLLEY, PRODUTOS, Permission.CONSULTAR, false))));

        service.saveUserGrid(WESLLEY, new GridDTO(Map.of(PRODUTOS, List.of("CONSULTAR"))));

        verify(permissions).forget(WESLLEY);
    }

    /**
     * **E gravar um MODELO não esquece o cache de ninguém — de propósito.**
     *
     * O carimbo já passou: quem foi carimbado com ele não muda porque ele
     * mudou. Invalidar aqui daria a impressão contrária, e é justamente a
     * confusão que o desenho de "modelo é carimbo, não pai" existe para evitar.
     */
    @Test
    @DisplayName("gravar um modelo não invalida cache de ninguém")
    void gravarModeloNaoInvalidaNinguem() {
        when(templateCells.findByTemplateId(ESTOQUE)).thenReturn(new ArrayList<>(List.of(
                celulaDoModelo(ESTOQUE, PRODUTOS, Permission.CONSULTAR))));

        service.saveTemplateGrid(ESTOQUE, new GridDTO(Map.of()));

        verify(permissions, never()).forget(anyString());
        verify(permissions, never()).forgetAll();
    }

    // ─── A gravação da grade ─────────────────────────────────────────────────

    /**
     * **Ausente é negado**, e é o que torna o `PUT` idempotente.
     *
     * A tela manda só o que está ligado. Se ausência significasse "não mexer",
     * desmarcar uma célula não teria como ser expresso — o corpo ficaria igual
     * ao de antes.
     */
    @Test
    @DisplayName("o que não veio no corpo é desligado")
    void ausenteEhNegado() {
        List<UserPermission> grade = new ArrayList<>(List.of(
                celula(WESLLEY, MOVIMENTACOES, Permission.EXCLUIR, true),
                celula(WESLLEY, MOVIMENTACOES, Permission.CONSULTAR, true)));
        when(userCells.findAllOfUser(WESLLEY)).thenReturn(grade);

        service.saveUserGrid(WESLLEY, new GridDTO(Map.of(MOVIMENTACOES, List.of("CONSULTAR"))));

        assertThat(ligada(grade, MOVIMENTACOES, Permission.EXCLUIR)).isFalse();
        assertThat(ligada(grade, MOVIMENTACOES, Permission.CONSULTAR)).isTrue();
    }

    /**
     * Permissão que o enum não conhece não derruba a gravação.
     *
     * É tela velha mandando o que não existe mais. Recusar o corpo inteiro
     * trocaria um problema invisível por um que impede de trabalhar.
     */
    @Test
    @DisplayName("permissão desconhecida é ignorada, e o resto grava")
    void permissaoDesconhecidaNaoDerruba() {
        List<UserPermission> grade = new ArrayList<>(List.of(
                celula(WESLLEY, MOVIMENTACOES, Permission.CONSULTAR, false)));
        when(userCells.findAllOfUser(WESLLEY)).thenReturn(grade);

        service.saveUserGrid(WESLLEY,
                new GridDTO(Map.of(MOVIMENTACOES, List.of("REPASSAR", "CONSULTAR"))));

        assertThat(ligada(grade, MOVIMENTACOES, Permission.CONSULTAR)).isTrue();
    }

    // ─── Cliente ─────────────────────────────────────────────────────────────

    /**
     * O portal do cliente tem escopo próprio, decidido pela API.
     *
     * A V86 deixou o cliente fora de `user_permissions` de propósito. Sem esta
     * recusa, a tela de configuração criaria essas linhas pela porta dos fundos
     * e passaria a sugerir que o portal responde a elas — e ele não responde.
     */
    @Test
    @DisplayName("cliente não tem grade de tela, e a tela recusa")
    void clienteEhRecusado() {
        when(users.findById("u-cliente")).thenReturn(Optional.of(cliente("u-cliente")));

        assertThatThrownBy(() -> service.userGrid("u-cliente"))
                .isInstanceOf(ClientHasNoScreenPermissionsException.class);
    }

    // ─── Copiar de outra pessoa ──────────────────────────────────────────────

    /**
     * Copiar leva o histórico junto, e não é detalhe.
     *
     * Sem os carimbos, a tela do destino calcularia o esperado a partir de
     * modelos que ele nunca recebeu — e apontaria divergência em toda célula,
     * transformando o ponto âmbar em ruído.
     */
    @Test
    @DisplayName("copiar de outra pessoa leva a grade e os carimbos")
    void copiarLevaOsCarimbos() {
        when(userCells.findAllOfUser(RICARDO)).thenReturn(new ArrayList<>(List.of(
                celula(RICARDO, PRODUTOS, Permission.CONSULTAR, true))));
        List<UserPermission> destino = new ArrayList<>(List.of(
                celula(WESLLEY, PRODUTOS, Permission.CONSULTAR, false)));
        when(userCells.findAllOfUser(WESLLEY)).thenReturn(destino);
        when(stamps.findByUserId(RICARDO)).thenReturn(List.of(
                new UserTemplate(RICARDO, ESTOQUE, "murillo", ApplyMode.SOMAR)));

        service.copyFrom(WESLLEY, RICARDO);

        assertThat(ligada(destino, PRODUTOS, Permission.CONSULTAR)).isTrue();
        verify(stamps).deleteByUserId(WESLLEY);

        ArgumentCaptor<UserTemplate> copiado = ArgumentCaptor.forClass(UserTemplate.class);
        verify(stamps).save(copiado.capture());
        assertThat(copiado.getValue().getUserId()).isEqualTo(WESLLEY);
        assertThat(copiado.getValue().getTemplateId()).isEqualTo(ESTOQUE);
        verify(permissions).forget(WESLLEY);
    }

    // ─── Criar e duplicar ────────────────────────────────────────────────────

    /**
     * Duplicar é criar com uma origem — não é outro endpoint.
     *
     * Do zero são 385 cliques na tela. Se o modelo novo nascesse vazio, ninguém
     * usaria a função e todo modelo parecido continuaria sendo montado à mão.
     */
    @Test
    @DisplayName("duplicar nasce com as células do original ligadas")
    void duplicarNasceCopiado() {
        UUID novo = UUID.randomUUID();
        when(templates.findByName("ESTOQUE JÚNIOR")).thenReturn(Optional.empty());
        when(templates.saveAndFlush(any(PermissionTemplate.class))).thenAnswer(invocacao -> {
            PermissionTemplate template = invocacao.getArgument(0);
            template.setId(novo);
            return template;
        });
        when(screens.findAll()).thenReturn(List.of(tela(PRODUTOS), tela(MOVIMENTACOES)));
        modeloEstoqueLibera(PRODUTOS, Permission.CONSULTAR);

        TemplateSummaryDTO resumo = service.create(
                new TemplateFormDTO("ESTOQUE JÚNIOR", "cópia de ESTOQUE", ESTOQUE));

        assertThat(resumo.allowedCells())
                .as("só a célula que o original tinha ligada")
                .isEqualTo(1);
        assertThat(resumo.stampedUsers()).isZero();

        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<TemplatePermission>> criadas = ArgumentCaptor.forClass(List.class);
        verify(templateCells).saveAll(criadas.capture());
        assertThat(criadas.getValue())
                .as("as duas telas vezes as sete permissões")
                .hasSize(14);
    }

    @Test
    @DisplayName("nome repetido não cria um segundo modelo")
    void nomeRepetidoRecusa() {
        when(templates.findByName("ESTOQUE")).thenReturn(Optional.of(modelo(ESTOQUE, "ESTOQUE")));

        assertThatThrownBy(() -> service.create(new TemplateFormDTO("ESTOQUE", null, null)))
                .isInstanceOf(PermissionTemplateNameInUseException.class);

        verify(templates, never()).saveAndFlush(any());
    }

    private static Screen tela(String code) {
        Screen screen = new Screen();
        screen.setCode(code);
        screen.setLabel(code);
        screen.setModule("Estoque");
        return screen;
    }
}
