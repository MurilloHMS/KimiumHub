package com.proautokimium.api.Infrastructure.settings;

import com.proautokimium.api.Infrastructure.services.permission.PermissionService;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.concurrent.ConcurrentMapCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * O cache das permissões.
 *
 * `ConcurrentMapCacheManager` e não Caffeine ou Redis: **não existe expiração
 * por tempo aqui, de propósito**. Permissão é o tipo de coisa em que "vale em
 * cinco minutos" é resposta errada — quem tirou o acesso de alguém espera que
 * tenha sido agora. A invalidação é por escrita, e quem escreve chama o
 * `PermissionService.forget`.
 *
 * A consequência de não ter TTL: em mais de uma instância, cada uma tem o
 * próprio mapa, e o `forget` de uma não alcança a outra. Enquanto a API roda
 * numa instância só isso não aparece; quando não rodar mais, este é o arquivo
 * que precisa virar Redis.
 */
@Configuration
@EnableCaching
public class CacheConfig {

    @Bean
    public CacheManager cacheManager() {
        return new ConcurrentMapCacheManager(PermissionService.CACHE);
    }
}
