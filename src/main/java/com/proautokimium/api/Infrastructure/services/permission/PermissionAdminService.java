package com.proautokimium.api.Infrastructure.services.permission;

import com.proautokimium.api.Application.DTOs.permission.PermissionDTOs.*;
import com.proautokimium.api.Infrastructure.repositories.UserRepository;
import com.proautokimium.api.Infrastructure.repositories.permission.*;
import com.proautokimium.api.domain.entities.auth.User;
import com.proautokimium.api.domain.entities.permission.*;
import com.proautokimium.api.domain.enums.ApplyMode;
import com.proautokimium.api.domain.enums.Permission;
import com.proautokimium.api.domain.enums.UserRole;
import com.proautokimium.api.domain.exceptions.auth.UserNotFoundException;
import com.proautokimium.api.domain.exceptions.permission.ClientHasNoScreenPermissionsException;
import com.proautokimium.api.domain.exceptions.permission.PermissionTemplateNameInUseException;
import com.proautokimium.api.domain.exceptions.permission.PermissionTemplateNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

/**
 * As telas que configuram o controle de acesso.
 *
 * A regra que organiza tudo aqui: **aplicar um modelo é copiar, não vincular.**
 * A aplicação escreve as permissões dentro da pessoa, e depois disso o modelo
 * não manda mais nada nela. Por isso mexer num modelo não invalida cache de
 * ninguém, e mexer numa pessoa invalida só o dela.
 *
 * `user_templates` guarda só o **registro de que a cópia aconteceu** — para a
 * tela poder avisar quem já recebeu, oferecer reaplicar, e desfazer.
 */
@Service
public class PermissionAdminService {

    private final ScreenRepository screens;
    private final PermissionTemplateRepository templates;
    private final TemplatePermissionRepository templateCells;
    private final UserPermissionRepository userCells;
    private final UserTemplateRepository applied;
    private final UserRepository users;
    private final PermissionService permissions;

    public PermissionAdminService(ScreenRepository screens,
                                  PermissionTemplateRepository templates,
                                  TemplatePermissionRepository templateCells,
                                  UserPermissionRepository userCells,
                                  UserTemplateRepository applied,
                                  UserRepository users,
                                  PermissionService permissions) {
        this.screens = screens;
        this.templates = templates;
        this.templateCells = templateCells;
        this.userCells = userCells;
        this.applied = applied;
        this.users = users;
        this.permissions = permissions;
    }

    // ─── Catálogo ────────────────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public List<ScreenDTO> screens() {
        return screens.findByActiveTrueOrderByModuleAscSortOrderAsc().stream()
                .map(s -> new ScreenDTO(s.getCode(), s.getLabel(), s.getModule(), s.getSortOrder()))
                .toList();
    }

    // ─── Modelos ─────────────────────────────────────────────────────────────

    /**
     * A lista lateral: os modelos com o placar de cada um.
     *
     * Devolve inativos também. Um modelo desativado que some da tela vira um
     * modelo que ninguém consegue reativar.
     */
    @Transactional(readOnly = true)
    public List<TemplateSummaryDTO> templates() {
        Map<UUID, Long> ligadas = new HashMap<>();
        templateCells.countAllowedByTemplate()
                .forEach(c -> ligadas.put(c.getTemplateId(), c.getTotal()));

        Map<UUID, Long> alcancados = new HashMap<>();
        applied.countByTemplate()
                .forEach(c -> alcancados.put(c.getTemplateId(), c.getTotal()));

        return templates.findAll().stream()
                .sorted(Comparator.comparing(PermissionTemplate::getName))
                .map(t -> new TemplateSummaryDTO(
                        t.getId(), t.getName(), t.getDescription(), t.isActive(),
                        ligadas.getOrDefault(t.getId(), 0L),
                        alcancados.getOrDefault(t.getId(), 0L)))
                .toList();
    }

    /**
     * A quem este modelo já foi aplicado.
     *
     * A tela de modelos precisa dos **nomes**, para o aviso, e dos **ids**,
     * para o reaplicar. Sem eles o aviso diria "3 usuários" e o botão ao lado
     * não teria em quem mexer — que é o mesmo que não ter botão.
     */
    @Transactional(readOnly = true)
    public List<UserSummaryDTO> appliedTo(UUID templateId) {
        templates.findById(templateId).orElseThrow(PermissionTemplateNotFoundException::new);

        Set<String> alcancados = new HashSet<>();
        applied.findByTemplateId(templateId).forEach(reg -> alcancados.add(reg.getUserId()));
        if (alcancados.isEmpty()) return List.of();

        return users().stream().filter(u -> alcancados.contains(u.id())).toList();
    }

    @Transactional(readOnly = true)
    public TemplateGridDTO templateGrid(UUID templateId) {
        PermissionTemplate template = templates.findById(templateId)
                .orElseThrow(PermissionTemplateNotFoundException::new);

        Map<String, List<String>> cells = new LinkedHashMap<>();
        for (TemplatePermission cell : templateCells.findByTemplateIdAndAllowedTrue(templateId)) {
            cells.computeIfAbsent(cell.getScreenCode(), tela -> new ArrayList<>())
                    .add(cell.getPermission().name());
        }

        return new TemplateGridDTO(template.getId(), template.getName(),
                template.getDescription(), template.isActive(), cells);
    }

    /**
     * Cria um modelo — e `copyFromId` preenchido **é** o duplicar.
     *
     * Não é um endpoint separado porque não é um ato separado: duplicar é criar
     * com uma origem. Do zero são 385 cliques na tela; a partir de uma cópia,
     * cinco — e é por isso que o botão fica ao lado, e não escondido num menu.
     */
    @Transactional
    public TemplateSummaryDTO create(TemplateFormDTO form) {
        String nome = form.name() == null ? "" : form.name().trim();
        if (nome.isEmpty()) {
            throw new IllegalArgumentException("O modelo precisa de um nome.");
        }
        templates.findByName(nome).ifPresent(existente -> {
            throw new PermissionTemplateNameInUseException(nome);
        });

        PermissionTemplate template = new PermissionTemplate();
        template.setName(nome);
        template.setDescription(form.description());
        templates.saveAndFlush(template);

        // De onde as células vêm ligadas, se for cópia.
        Set<String> origem = form.copyFromId() == null
                ? Set.of()
                : allowedKeysOfTemplate(form.copyFromId());

        // As 385 células nascem aqui, e não pela sincronização do boot: um
        // modelo criado hoje precisa aparecer na tela agora, não na próxima
        // subida da API.
        List<TemplatePermission> novas = new ArrayList<>();
        for (Screen screen : screens.findAll()) {
            for (Permission permission : Permission.values()) {
                TemplatePermission cell = new TemplatePermission();
                cell.setTemplateId(template.getId());
                cell.setScreenCode(screen.getCode());
                cell.setPermission(permission);
                cell.setAllowed(origem.contains(key(screen.getCode(), permission)));
                novas.add(cell);
            }
        }
        templateCells.saveAll(novas);

        long ligadas = novas.stream().filter(TemplatePermission::isAllowed).count();
        return new TemplateSummaryDTO(template.getId(), template.getName(),
                template.getDescription(), template.isActive(), ligadas, 0L);
    }

    @Transactional
    public void edit(UUID templateId, TemplateEditDTO form) {
        PermissionTemplate template = templates.findById(templateId)
                .orElseThrow(PermissionTemplateNotFoundException::new);

        if (form.name() != null && !form.name().isBlank()) {
            String nome = form.name().trim();
            templates.findByName(nome).ifPresent(outro -> {
                if (!outro.getId().equals(templateId)) {
                    throw new PermissionTemplateNameInUseException(nome);
                }
            });
            template.setName(nome);
        }
        if (form.description() != null) template.setDescription(form.description());
        if (form.active() != null) template.setActive(form.active());

        templates.save(template);
    }

    /**
     * Grava a grade de um modelo.
     *
     * **Não invalida cache de ninguém, e isso não é esquecimento.** A cópia já
     * aconteceu: quem recebeu este modelo não muda porque ele mudou. Alcançar
     * essas pessoas é o "reaplicar", que é um ato explícito e avisado.
     */
    @Transactional
    public int saveTemplateGrid(UUID templateId, GridDTO grid) {
        templates.findById(templateId).orElseThrow(PermissionTemplateNotFoundException::new);

        Set<String> desejadas = keysOf(grid);
        List<TemplatePermission> cells = templateCells.findByTemplateId(templateId);

        int alteradas = 0;
        for (TemplatePermission cell : cells) {
            boolean deveria = desejadas.contains(key(cell.getScreenCode(), cell.getPermission()));
            if (cell.isAllowed() != deveria) {
                cell.setAllowed(deveria);
                alteradas++;
            }
        }
        templateCells.saveAll(cells);
        return alteradas;
    }

    // ─── Pessoas ─────────────────────────────────────────────────────────────

    /**
     * As pessoas que participam deste sistema — cliente fora.
     *
     * O portal do cliente tem sessão e escopo próprios; listá-lo aqui sugeriria
     * que dá para configurá-lo por tela, e não dá.
     */
    @Transactional(readOnly = true)
    public List<UserSummaryDTO> users() {
        Map<UUID, String> nomeDoModelo = new HashMap<>();
        templates.findAll().forEach(t -> nomeDoModelo.put(t.getId(), t.getName()));

        Map<String, List<String>> modelosDaPessoa = new HashMap<>();
        for (UserTemplate registro : applied.findAll()) {
            modelosDaPessoa.computeIfAbsent(registro.getUserId(), id -> new ArrayList<>())
                    .add(nomeDoModelo.getOrDefault(registro.getTemplateId(), "?"));
        }

        return users.findAllWithEmployee().stream()
                .filter(u -> !u.getRoles().contains(UserRole.CLIENTE))
                .sorted(Comparator.comparing(PermissionAdminService::displayName,
                        String.CASE_INSENSITIVE_ORDER))
                .map(u -> new UserSummaryDTO(u.getId(), displayName(u), u.getLogin(),
                        u.isActive(),
                        modelosDaPessoa.getOrDefault(u.getId(), List.of()).stream().sorted().toList()))
                .toList();
    }

    @Transactional(readOnly = true)
    public UserGridDTO userGrid(String userId) {
        User user = requireUser(userId);

        Map<String, List<String>> cells = new LinkedHashMap<>();
        for (UserPermission cell : userCells.findAllOfUser(userId)) {
            if (!cell.isAllowed()) continue;
            cells.computeIfAbsent(cell.getScreenCode(), tela -> new ArrayList<>())
                    .add(cell.getPermission().name());
        }

        List<UserTemplate> aplicados = applied.findByUserId(userId);

        Map<UUID, PermissionTemplate> porId = new HashMap<>();
        templates.findAll().forEach(t -> porId.put(t.getId(), t));

        // O esperado é a UNIÃO do que os modelos aplicados permitem — semântica
        // de SOMAR, que é como quase toda aplicação acontece. A divergência com
        // `cells` é o ponto âmbar da tela.
        //
        // O que ele NÃO distingue: célula que divergiu porque alguém a ajustou,
        // e célula que divergiu porque o modelo mudou depois. Por isso a tela
        // diz "difere dos modelos aplicados", e não "ajuste individual".
        Map<String, List<String>> peloModelo = new LinkedHashMap<>();
        for (UserTemplate registro : aplicados) {
            for (TemplatePermission cell : templateCells.findByTemplateIdAndAllowedTrue(registro.getTemplateId())) {
                List<String> naTela = peloModelo.computeIfAbsent(
                        cell.getScreenCode(), tela -> new ArrayList<>());
                if (!naTela.contains(cell.getPermission().name())) {
                    naTela.add(cell.getPermission().name());
                }
            }
        }

        List<AppliedTemplateDTO> historico = aplicados.stream()
                .map(registro -> new AppliedTemplateDTO(
                        registro.getTemplateId(),
                        porId.containsKey(registro.getTemplateId())
                                ? porId.get(registro.getTemplateId()).getName() : "?",
                        registro.getAppliedAt(), registro.getAppliedBy(), registro.getMode()))
                .sorted(Comparator.comparing(AppliedTemplateDTO::name))
                .toList();

        return new UserGridDTO(user.getId(), displayName(user), user.getLogin(),
                cells, peloModelo, historico);
    }

    /**
     * Grava a grade de uma pessoa — e **esquece o cache dela**.
     *
     * Sem o `forget`, esta é a feature inteira falhando do jeito mais chato: a
     * permissão muda no banco, a tela mostra o valor novo, e a API continua
     * recusando até a pessoa sair e entrar.
     */
    @Transactional
    public int saveUserGrid(String userId, GridDTO grid) {
        requireUser(userId);

        Set<String> desejadas = keysOf(grid);
        List<UserPermission> cells = userCells.findAllOfUser(userId);

        int alteradas = 0;
        for (UserPermission cell : cells) {
            boolean deveria = desejadas.contains(key(cell.getScreenCode(), cell.getPermission()));
            if (cell.isAllowed() != deveria) {
                cell.setAllowed(deveria);
                alteradas++;
            }
        }
        userCells.saveAll(cells);
        permissions.forget(userId);
        return alteradas;
    }

    /**
     * Aplica um modelo a N pessoas.
     *
     * SOMAR liga o que o modelo permite e não desliga nada — é o que faz
     * "Vendas + Estoque" funcionar sem existir um modelo combinado. SUBSTITUIR
     * grava o modelo exato, e é o único caminho pelo qual um ajuste individual
     * se perde.
     *
     * Feito em Java, e não num `UPDATE ... FROM`: essa sintaxe é do Postgres e
     * os testes rodam em H2. Quatro pessoas são 1.540 células — o custo de
     * fazer certo aqui é invisível, e o de manter duas variantes de SQL com só
     * uma delas exercitada não seria.
     */
    @Transactional
    public ApplyResultDTO apply(UUID templateId, ApplyTemplateDTO form, String appliedBy) {
        templates.findById(templateId).orElseThrow(PermissionTemplateNotFoundException::new);

        List<String> alvos = form.userIds() == null ? List.of() : form.userIds();
        for (String userId : alvos) requireUser(userId);
        if (alvos.isEmpty()) return new ApplyResultDTO(0, 0);

        ApplyMode modo = form.mode() == null ? ApplyMode.SOMAR : form.mode();
        Set<String> doModelo = allowedKeysOfTemplate(templateId);

        List<UserPermission> cells = userCells.findByUserIdIn(alvos);
        int alteradas = 0;
        for (UserPermission cell : cells) {
            String chave = key(cell.getScreenCode(), cell.getPermission());
            boolean deveria = modo == ApplyMode.SUBSTITUIR
                    ? doModelo.contains(chave)
                    : cell.isAllowed() || doModelo.contains(chave);

            if (cell.isAllowed() != deveria) {
                cell.setAllowed(deveria);
                alteradas++;
            }
        }
        userCells.saveAll(cells);

        // Registra a aplicação. Aplicar de novo na mesma pessoa atualiza a data
        // e o modo em vez de criar uma segunda linha — a chave é (usuário,
        // modelo), e duas linhas iguais não diriam nada a mais.
        for (String userId : alvos) {
            applied.save(new UserTemplate(userId, templateId, appliedBy, modo));
            permissions.forget(userId);
        }

        return new ApplyResultDTO(alvos.size(), alteradas);
    }

    /**
     * "Deixa o Pedro igual ao João."
     *
     * Copia a grade **e a lista de modelos aplicados**. Copiar só a grade
     * deixaria a tela do Pedro apontando divergência em toda célula, porque o
     * esperado seria calculado a partir de modelos que ele nunca recebeu.
     */
    @Transactional
    public int copyFrom(String targetUserId, String sourceUserId) {
        requireUser(targetUserId);
        requireUser(sourceUserId);

        Set<String> origem = new HashSet<>();
        for (UserPermission cell : userCells.findAllOfUser(sourceUserId)) {
            if (cell.isAllowed()) origem.add(key(cell.getScreenCode(), cell.getPermission()));
        }

        List<UserPermission> cells = userCells.findAllOfUser(targetUserId);
        int alteradas = 0;
        for (UserPermission cell : cells) {
            boolean deveria = origem.contains(key(cell.getScreenCode(), cell.getPermission()));
            if (cell.isAllowed() != deveria) {
                cell.setAllowed(deveria);
                alteradas++;
            }
        }
        userCells.saveAll(cells);

        List<UserTemplate> daOrigem = applied.findByUserId(sourceUserId);
        applied.deleteByUserId(targetUserId);
        for (UserTemplate registro : daOrigem) {
            applied.save(new UserTemplate(targetUserId, registro.getTemplateId(),
                    registro.getAppliedBy(), registro.getMode()));
        }

        permissions.forget(targetUserId);
        return alteradas;
    }

    /**
     * Desfaz a aplicação de um modelo numa pessoa.
     *
     * **Não é apagar o registro e pronto.** Apagar só a linha de
     * `user_templates` não tiraria permissão nenhuma — ela é anotação, não
     * fonte. Desfazer de verdade é desligar o que **aquele** modelo deu.
     *
     * A parte que importa é o `manter`: uma permissão que outro modelo aplicado
     * também dá **fica ligada**. Sem isso, desfazer ALMOXARIFADO no Weslley
     * derrubaria junto as telas que o Base dá, e a pessoa sairia com menos do
     * que tinha antes de qualquer modelo — o oposto de desfazer.
     *
     * O que ele não alcança: permissão que alguém ligou à mão depois e que por
     * acaso o modelo também dava. Ela é desligada. É o mesmo limite do ponto
     * âmbar — o sistema guarda que a cópia aconteceu, não o que havia antes
     * dela.
     */
    @Transactional
    public ApplyResultDTO undoApply(String userId, UUID templateId) {
        requireUser(userId);
        templates.findById(templateId).orElseThrow(PermissionTemplateNotFoundException::new);

        Set<String> desteModelo = allowedKeysOfTemplate(templateId);

        Set<String> manter = new HashSet<>();
        for (UserTemplate registro : applied.findByUserId(userId)) {
            if (registro.getTemplateId().equals(templateId)) continue;
            manter.addAll(allowedKeysOfTemplate(registro.getTemplateId()));
        }

        List<UserPermission> cells = userCells.findAllOfUser(userId);
        int alteradas = 0;
        for (UserPermission cell : cells) {
            String chave = key(cell.getScreenCode(), cell.getPermission());
            if (cell.isAllowed() && desteModelo.contains(chave) && !manter.contains(chave)) {
                cell.setAllowed(false);
                alteradas++;
            }
        }
        userCells.saveAll(cells);

        UserTemplate.Key chave = new UserTemplate.Key();
        chave.setUserId(userId);
        chave.setTemplateId(templateId);
        applied.deleteById(chave);

        permissions.forget(userId);
        return new ApplyResultDTO(1, alteradas);
    }

    // ─── Miudezas ────────────────────────────────────────────────────────────

    /** O nome do funcionário, e o login como último recurso. */
    private static String displayName(User user) {
        return user.getEmployee() != null && user.getEmployee().getName() != null
                ? user.getEmployee().getName()
                : user.getLogin();
    }

    /**
     * Carrega a pessoa e recusa cliente.
     *
     * O portal do cliente tem escopo próprio e ele não tem linha em
     * `user_permissions` — a V86 o deixou de fora de propósito. Recusar aqui é
     * o que impede a tela de configuração de criar essas linhas pela porta dos
     * fundos e passar a sugerir que o portal responde a elas.
     */
    private User requireUser(String userId) {
        User user = users.findById(userId).orElseThrow(UserNotFoundException::new);
        if (user.getRoles().contains(UserRole.CLIENTE)) {
            throw new ClientHasNoScreenPermissionsException();
        }
        return user;
    }

    private Set<String> allowedKeysOfTemplate(UUID templateId) {
        Set<String> chaves = new HashSet<>();
        for (TemplatePermission cell : templateCells.findByTemplateIdAndAllowedTrue(templateId)) {
            chaves.add(key(cell.getScreenCode(), cell.getPermission()));
        }
        return chaves;
    }

    /**
     * O que veio no corpo, achatado em `tela:PERMISSÃO`.
     *
     * Permissão desconhecida é ignorada em silêncio de propósito: um valor que
     * o enum não tem é tela velha mandando o que não existe mais, e derrubar a
     * gravação inteira por causa disso trocaria um problema invisível por um
     * que impede de trabalhar.
     */
    private static Set<String> keysOf(GridDTO grid) {
        Set<String> chaves = new HashSet<>();
        if (grid == null || grid.cells() == null) return chaves;

        grid.cells().forEach((tela, permissoes) -> {
            if (permissoes == null) return;
            for (String nome : permissoes) {
                try {
                    chaves.add(key(tela, Permission.valueOf(nome)));
                } catch (IllegalArgumentException ignorada) {
                    // Permissão que o enum não conhece.
                }
            }
        });
        return chaves;
    }

    private static String key(String screenCode, Permission permission) {
        return screenCode + ":" + permission.name();
    }
}
