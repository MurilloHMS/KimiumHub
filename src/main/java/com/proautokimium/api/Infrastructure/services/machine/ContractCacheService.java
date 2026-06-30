package com.proautokimium.api.Infrastructure.services.machine;

import com.proautokimium.api.domain.models.MachineContract;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Service
public class ContractCacheService {

    private static final long TTL_MILLIS = Duration.ofHours(2).toMillis(); // 2 horas

    private record CacheEntry(List<MachineContract> contracts, Instant expiresAt) {}

    private final Map<String, CacheEntry> cache = new ConcurrentHashMap<>();

    /** Armazena os contratos e retorna o processId. */
    public String store(List<MachineContract> contracts) {
        evictExpired();
        String processId = UUID.randomUUID().toString();
        cache.put(processId, new CacheEntry(contracts, Instant.now().plusMillis(TTL_MILLIS)));
        log.info("planilha armazenada em cache.");
        return processId;
    }

    /**
     * Recupera os contratos pelo processId.
     * @throws IllegalArgumentException se não encontrado ou expirado.
     */
    public List<MachineContract> get(String processId) {
        CacheEntry entry = cache.get(processId);

        if (entry == null || Instant.now().isAfter(entry.expiresAt())) {
            cache.remove(processId);
            log.warn("Sessão expirada ou inválida. Faça o upload da planilha novamente.");
            throw new IllegalArgumentException(
                    "Sessão expirada ou inválida. Faça o upload da planilha novamente."
            );
        }

        return entry.contracts();
    }

    /** Remove do cache após geração bem-sucedida. */
    public void evict(String processId) {
        log.info("planilha removida com sucesso.");
        cache.remove(processId);
    }

    private void evictExpired() {
        Instant now = Instant.now();
        cache.entrySet().removeIf(e -> now.isAfter(e.getValue().expiresAt()));
    }
}