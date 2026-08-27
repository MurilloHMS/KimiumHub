package com.proautokimium.api.Infrastructure.services.permission;

import com.proautokimium.api.Infrastructure.repositories.UserRepository;
import com.proautokimium.api.Infrastructure.repositories.permission.ScreenRepository;
import com.proautokimium.api.Infrastructure.repositories.permission.UserPermissionRepository;
import com.proautokimium.api.Infrastructure.settings.CacheConfig;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * O cache, e a invalidação dele.
 *
 * **A invalidação é a parte que assusta.** Se ela falhar, a permissão muda no
 * banco, a tela de configuração mostra o novo valor, e a API continua recusando
 * até a pessoa sair e entrar — um defeito que parece bug de front, some quando
 * alguém vai investigar, e volta no dia seguinte.
 *
 * Por isso o cache não tem expiração por tempo: "vale em cinco minutos" é
 * resposta errada para permissão. Quem tirou o acesso de alguém espera que
 * tenha sido agora.
 */
/*
 * Contexto MÍNIMO: só o cache e o serviço.
 *
 * `@SpringBootTest` sem `classes` subiria a aplicação inteira, e ela não sobe
 * no perfil de teste — falta um `JavaMailSender`. Mas mesmo que subisse, erguer
 * 54 controllers para exercitar um cache seria caro por nada.
 */
@SpringBootTest(classes = { CacheConfig.class, PermissionService.class })
@ActiveProfiles("test")
class PermissionCacheTest {

    @Autowired private PermissionService service;
    @MockitoBean private UserPermissionRepository repository;
    @MockitoBean private UserRepository users;
    @MockitoBean private ScreenRepository screens;

    @Test
    @DisplayName("A segunda leitura vem do cache, sem ir ao banco")
    void segundaLeituraVemDoCache() {
        when(repository.findAuthorities("u-cache")).thenReturn(List.of("rh/hub:CONSULTAR"));

        service.authoritiesOf("u-cache");
        service.authoritiesOf("u-cache");

        verify(repository, times(1)).findAuthorities("u-cache");
    }

    /**
     * **O teste que importa.**
     *
     * Sem ele, o `@CacheEvict` pode estar com a chave errada e ninguém percebe:
     * o método não devolve nada, não lança nada, e o defeito só aparece com
     * alguém reclamando que "tirei a permissão e ele continua entrando".
     */
    @Test
    @DisplayName("Depois do forget, a leitura volta ao banco")
    void forgetFazVoltarAoBanco() {
        when(repository.findAuthorities("u-evict")).thenReturn(List.of("rh/hub:CONSULTAR"));
        service.authoritiesOf("u-evict");

        service.forget("u-evict");
        service.authoritiesOf("u-evict");

        verify(repository, times(2)).findAuthorities("u-evict");
    }

    /** Esquecer um não pode esquecer o outro — senão o cache não serve para nada. */
    @Test
    @DisplayName("O forget de um usuário não derruba o cache do outro")
    void forgetNaoDerrubaOsOutros() {
        when(repository.findAuthorities("u-a")).thenReturn(List.of("rh/hub:CONSULTAR"));
        when(repository.findAuthorities("u-b")).thenReturn(List.of("stock/hub:CONSULTAR"));

        service.authoritiesOf("u-a");
        service.authoritiesOf("u-b");
        service.forget("u-a");
        service.authoritiesOf("u-b");

        verify(repository, times(1)).findAuthorities("u-b");
    }

    /** Reaplicar um modelo em massa muda muita gente de uma vez. */
    @Test
    @DisplayName("forgetAll esquece todo mundo")
    void forgetAllEsqueceTodos() {
        when(repository.findAuthorities("u-x")).thenReturn(List.of("rh/hub:CONSULTAR"));
        service.authoritiesOf("u-x");

        service.forgetAll();
        service.authoritiesOf("u-x");

        verify(repository, times(2)).findAuthorities("u-x");
    }

    @Test
    @DisplayName("Sem permissão, o cache guarda o vazio e não vai ao banco de novo")
    void cacheGuardaOVazio() {
        when(repository.findAuthorities("u-vazio")).thenReturn(List.of());

        assertThat(service.authoritiesOf("u-vazio")).isEmpty();
        assertThat(service.authoritiesOf("u-vazio")).isEmpty();

        verify(repository, times(1)).findAuthorities("u-vazio");
    }
}
