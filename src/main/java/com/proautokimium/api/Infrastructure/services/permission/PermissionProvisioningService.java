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
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

/**
 * Dá a uma pessoa a grade de permissões que ela precisa ter para existir na
 * tela de acessos.
 *
 * <h2>O defeito que isto fecha</h2>
 *
 * As células de {@code user_permissions} nasciam **só na migration V86**, que
 * preencheu quem existia naquele momento. Nada em Java jamais criou uma linha.
 * Quem entrou depois — todo primeiro acesso desde então — ficava com a grade
 * vazia, e a partir daí:
 *
 * <ul>
 *   <li>{@code findAuthorities} devolvia lista vazia: nenhuma tela, nenhum
 *       acesso;</li>
 *   <li>a tela de permissões desenhava o catálogo inteiro desmarcado, com cara
 *       de normal, porque ela monta as 55 telas do catálogo e só marca o que
 *       vem do banco;</li>
 *   <li>salvar não fazia nada. O {@code saveUserGrid} percorre as células
 *       EXISTENTES e vira o {@code allowed} de cada uma — com zero células, o
 *       laço não roda e a resposta é "0 células alteradas". Aplicar modelo
 *       falhava igual, pelo mesmo motivo.</li>
 * </ul>
 *
 * Ou seja: a pessoa ficava trancada e sem conserto pela tela.
 *
 * <h2>Por que ele preenche o que falta, e não "cria na criação"</h2>
 *
 * O método é **idempotente**: insere só a combinação que ainda não existe.
 * Chamar duas vezes não faz nada na segunda, chamar em quem já tem a grade
 * cheia não faz nada, e por isso chamar de mais lugares do que o mínimo é de
 * graça. Três coisas saem disso:
 *
 * <ol>
 *   <li>Quem está quebrado hoje se conserta sozinho na primeira vez que alguém
 *       abrir as permissões dele — sem SQL, sem migration.</li>
 *   <li>Cobre o caso da V87: quem foi criado entre uma migration e outra tem as
 *       telas antigas e não as novas. "Preencher o que falta" resolve os dois;
 *       "criar na criação" não resolveria nenhum.</li>
 *   <li>O ponto de chamada deixa de ser uma decisão frágil. Um quarto caminho
 *       de cadastro que esqueça de chamar ainda é consertado ao abrir a tela.</li>
 * </ol>
 *
 * <h2>Cliente fica de fora</h2>
 *
 * A regra já existia em dois lugares — o {@code WHERE NOT EXISTS ... 'CLIENTE'}
 * da V86 e a consulta que lista pessoas para a tela de acessos. Ela mora aqui
 * dentro, e não em cada chamador, porque os dois primeiros acessos (funcionário
 * e cliente) saem do MESMO {@code repository.save}: se a decisão ficasse no
 * chamador, ele teria que repetir o {@code if} e acertar sempre. A Área do
 * Cliente decide escopo do lado dela.
 */
@Service
public class PermissionProvisioningService {

    /** O modelo que todo funcionário recebe, como na V86. */
    private static final String MODELO_BASE = "Base";

    private final ScreenRepository screens;
    private final UserPermissionRepository userCells;
    private final PermissionTemplateRepository templates;
    private final TemplatePermissionRepository templateCells;

    public PermissionProvisioningService(ScreenRepository screens,
                                         UserPermissionRepository userCells,
                                         PermissionTemplateRepository templates,
                                         TemplatePermissionRepository templateCells) {
        this.screens = screens;
        this.userCells = userCells;
        this.templates = templates;
        this.templateCells = templateCells;
    }

    /**
     * Garante que a pessoa tenha a grade completa, e libera o que a V86 liberava.
     *
     * <p><b>Junta-se à transação de quem chama, e isto já foi o contrário.</b>
     *
     * Nasceu como {@code REQUIRES_NEW}, para escapar do {@code readOnly} do
     * {@code userGrid}, e quebrou a criação de usuário em produção com violação
     * de chave estrangeira. O motivo é isolamento: o {@code signInFirstAccess} é
     * {@code @Transactional} e salva o usuário dentro da transação dele; uma
     * transação NOVA não enxerga linha ainda não commitada por outra, então o
     * {@code user_id} não existia do lado de cá.
     *
     * Quem grava as células precisa estar na mesma transação de quem criou o
     * usuário. O {@code userGrid} deixou de ser {@code readOnly} para caber
     * nisso — e a anotação de lá descrevia mesmo algo que parou de ser verdade
     * no dia em que o conserto entrou no caminho de leitura.
     *
     * <p><b>Nenhum teste de unidade pega isto.</b> Mock não tem transação;
     * semântica de transação só falha contra banco de verdade.
     *
     * @return quantas células foram criadas — zero quando não havia nada a fazer
     */
    @Transactional
    public int provision(User user) {
        if (user == null || user.getId() == null || isClient(user)) return 0;

        List<UserPermission> criadas = createMissingCells(user.getId());
        if (criadas.isEmpty()) return 0;

        // Só o que acabou de nascer é liberado. Célula que já existia pode ter
        // sido ajustada à mão por alguém — reaplicar o modelo por cima
        // desfaria o ajuste, e o método deixaria de ser seguro de chamar.
        Set<String> liberar = allowedKeysOf(MODELO_BASE);
        for (UserRole role : user.getRoles()) {
            // USER não tem modelo de propósito: quer dizer "sem setor", e o que
            // essa pessoa vê é exatamente o Base.
            liberar.addAll(allowedKeysOf(role.name()));
        }

        for (UserPermission cell : criadas) {
            if (liberar.contains(key(cell.getScreenCode(), cell.getPermission()))) {
                cell.setAllowed(true);
            }
        }

        userCells.saveAll(criadas);
        return criadas.size();
    }

    /**
     * As células que faltam, ainda fechadas.
     *
     * O conjunto do que já existe é montado antes para a comparação ser uma
     * consulta só — 55 telas por 7 permissões são 385 verificações, e uma ida
     * ao banco por verificação seria a diferença entre imperceptível e visível.
     */
    private List<UserPermission> createMissingCells(String userId) {
        Set<String> existentes = new HashSet<>();
        for (UserPermission cell : userCells.findAllOfUser(userId)) {
            existentes.add(key(cell.getScreenCode(), cell.getPermission()));
        }

        List<UserPermission> novas = new ArrayList<>();
        for (Screen screen : screens.findAll()) {
            for (Permission permission : Permission.values()) {
                if (existentes.contains(key(screen.getCode(), permission))) continue;

                UserPermission cell = new UserPermission();
                cell.setUserId(userId);
                cell.setScreenCode(screen.getCode());
                cell.setPermission(permission);
                cell.setAllowed(false);
                novas.add(cell);
            }
        }
        return novas;
    }

    /**
     * As chaves que um modelo permite, pelo NOME.
     *
     * Pelo nome porque é assim que a V86 casava role com modelo: o modelo de
     * VENDEDOR chama-se "VENDEDOR". Role sem modelo correspondente devolve
     * conjunto vazio em vez de estourar — nem toda role tem um, e uma role nova
     * não pode derrubar a criação de usuário.
     */
    private Set<String> allowedKeysOf(String templateName) {
        Optional<PermissionTemplate> template = templates.findByName(templateName);
        if (template.isEmpty()) return new HashSet<>();

        Set<String> chaves = new HashSet<>();
        for (TemplatePermission cell : templateCells.findByTemplateIdAndAllowedTrue(template.get().getId())) {
            chaves.add(key(cell.getScreenCode(), cell.getPermission()));
        }
        return chaves;
    }

    private static boolean isClient(User user) {
        return user.getRoles() != null && user.getRoles().contains(UserRole.CLIENTE);
    }

    private static String key(String screenCode, Permission permission) {
        return screenCode + ":" + permission.name();
    }
}
