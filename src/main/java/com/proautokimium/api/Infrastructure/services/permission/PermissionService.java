package com.proautokimium.api.Infrastructure.services.permission;

import com.proautokimium.api.Infrastructure.repositories.UserRepository;
import com.proautokimium.api.Infrastructure.repositories.permission.ScreenRepository;
import com.proautokimium.api.Infrastructure.repositories.permission.UserPermissionRepository;
import com.proautokimium.api.domain.entities.permission.Screen;
import com.proautokimium.api.domain.enums.Permission;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * O que uma pessoa pode, no formato que o Spring Security entende.
 *
 * A resolução é uma leitura de `user_permissions` e mais nada: o modelo já
 * carimbou, e o que ficou escrito lá é o que vale. Sem join com modelo, sem
 * união de grupos, sem precedência — foi a decisão que tornou "o que o João
 * pode?" uma pergunta com uma resposta só.
 */
@Service
public class PermissionService {

    /** O nome do cache. Usado também no `@CacheEvict` de quem altera permissão. */
    public static final String CACHE = "userAuthorities";

    private final UserPermissionRepository repository;
    private final UserRepository users;
    private final ScreenRepository screens;

    public PermissionService(UserPermissionRepository repository,
                             UserRepository users,
                             ScreenRepository screens) {
        this.repository = repository;
        this.users = users;
        this.screens = screens;
    }

    /**
     * **O desenvolvedor não perde acesso, nunca.**
     *
     * Sem esta saída, a tela de permissões consegue trancar o próprio dono: um
     * "bloquear tudo" na pessoa errada, ou um SUBSTITUIR com o modelo errado, e
     * ninguém mais abre a configuração — o mesmo impasse que a V87 resolveu no
     * banco, agora alcançável por dois cliques.
     *
     * É o mesmo papel que o `AuthenticationService` já reconhece ao recusar
     * bloquear um desenvolvedor. Aqui ele passa a valer para permissão também.
     *
     * O preço, dito na frente: quem tem `DEVELOPER` tem **tudo**, e isso não se
     * revoga pela interface. Tirar é `DELETE FROM user_roles`.
     */
    private Collection<GrantedAuthority> tudoQueExiste() {
        List<GrantedAuthority> todas = new ArrayList<>();
        for (Screen screen : screens.findByActiveTrueOrderByModuleAscSortOrderAsc()) {
            for (Permission permission : Permission.values()) {
                todas.add(new SimpleGrantedAuthority(screen.getCode() + ":" + permission.name()));
            }
        }
        return todas;
    }

    /**
     * As authorities de uma pessoa: `stock/movements:EXCLUIR` e companhia.
     *
     * **Roda uma vez por requisição**, no `SecurityFilter`, e por isso é
     * cacheada. Sem o cache, cada chamada à API viraria uma consulta a mais —
     * e o `@PreAuthorize` de cada endpoint depende dela existir.
     *
     * O cache é invalidado por escrita, não por tempo: permissão é o tipo de
     * coisa em que "vale em cinco minutos" é resposta errada. Quem tirou o
     * acesso de alguém espera que tenha sido agora.
     */
    @Cacheable(value = CACHE, key = "#userId")
    @Transactional(readOnly = true)
    public Collection<? extends GrantedAuthority> authoritiesOf(String userId) {
        if (users.isDeveloper(userId)) {
            return tudoQueExiste();
        }

        List<String> codes = repository.findAuthorities(userId);
        return codes.stream()
                .map(code -> (GrantedAuthority) new SimpleGrantedAuthority(code))
                .toList();
    }

    /**
     * O que a pessoa pode, agrupado por tela — o formato que o front consome.
     *
     * Mapa e não lista: o guard precisa responder "entra nesta tela?" a cada
     * item de menu e a cada render, e com lista isso seria varrer duzentas
     * strings procurando prefixo. Com mapa é uma busca direta.
     *
     * Reaproveita o mesmo cache: é a mesma leitura, em outro arranjo.
     */
    @Transactional(readOnly = true)
    public Map<String, List<String>> permissionsByScreen(String userId) {
        Map<String, List<String>> porTela = new LinkedHashMap<>();

        for (GrantedAuthority authority : authoritiesOf(userId)) {
            String[] partes = authority.getAuthority().split(":", 2);
            // Role (`ROLE_ADMIN`) não é permissão de tela. Sem esta linha, ela
            // viraria uma "tela" chamada ROLE_ADMIN no mapa do front.
            if (partes.length != 2) continue;
            porTela.computeIfAbsent(partes[0], tela -> new ArrayList<>()).add(partes[1]);
        }

        return porTela;
    }

    /**
     * A pessoa pode esta ação nesta tela?
     *
     * Para quando a **regra é do domínio**, e não um `@PreAuthorize` na porta:
     * o cartão digital, por exemplo, é uma seção dentro de uma tela que todo
     * mundo abre. Recusar o endpoint inteiro seria errado — quem não pode criar
     * ainda precisa ler o próprio perfil.
     *
     * Passa pelo cache, como todo o resto.
     */
    @Transactional(readOnly = true)
    public boolean can(String userId, String screenCode, Permission permission) {
        String procurado = screenCode + ":" + permission.name();
        return authoritiesOf(userId).stream()
                .anyMatch(authority -> procurado.equals(authority.getAuthority()));
    }

    /**
     * Esquece o que estava guardado de uma pessoa.
     *
     * Quem mexe em permissão precisa chamar isto — a tela de configuração, o
     * carimbo de um modelo, a criação de um usuário. Esquecer aqui é o defeito
     * mais chato desta feature: a permissão muda no banco, a tela mostra o novo
     * valor, e a API continua recusando até a pessoa sair e entrar.
     */
    @CacheEvict(value = CACHE, key = "#userId")
    public void forget(String userId) {
        // O corpo é vazio de propósito: quem faz o trabalho é a anotação.
    }

    /** Esquece todo mundo — para quando um modelo é reaplicado em massa. */
    @CacheEvict(value = CACHE, allEntries = true)
    public void forgetAll() {
    }
}
