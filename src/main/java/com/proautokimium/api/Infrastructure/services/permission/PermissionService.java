package com.proautokimium.api.Infrastructure.services.permission;

import com.proautokimium.api.Infrastructure.repositories.permission.UserPermissionRepository;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collection;
import java.util.List;

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

    public PermissionService(UserPermissionRepository repository) {
        this.repository = repository;
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
        List<String> codes = repository.findAuthorities(userId);
        return codes.stream()
                .map(code -> (GrantedAuthority) new SimpleGrantedAuthority(code))
                .toList();
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
