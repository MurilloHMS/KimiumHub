package com.proautokimium.api.Infrastructure.services.machine;

import com.proautokimium.api.domain.models.MachineContract;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Cache em memória para armazenar os contratos parseados entre as chamadas
 * de /preview e /generate, evitando re-upload da planilha.
 *
 * TTL padrão: 30 minutos.
 * Para produção multi-instância, substituir por Redis.
 */
@Service
public class ContractCacheService {

    private static final long TTL_MILLIS = 30 * 60 * 1000L; // 30 min

    private record CacheEntry(List<MachineContract> contracts, Instant expiresAt) {}

    private final Map<String, CacheEntry> cache = new ConcurrentHashMap<>();

    /** Armazena os contratos e retorna o processId. */
    public String store(List<MachineContract> contracts) {
        evictExpired();
        String processId = UUID.randomUUID().toString();
        cache.put(processId, new CacheEntry(contracts, Instant.now().plusMillis(TTL_MILLIS)));
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
            throw new IllegalArgumentException(
                    "Sessão expirada ou inválida. Faça o upload da planilha novamente."
            );
        }

        return entry.contracts();
    }

    /** Remove do cache após geração bem-sucedida. */
    public void evict(String processId) {
        cache.remove(processId);
    }

    private void evictExpired() {
        Instant now = Instant.now();
        cache.entrySet().removeIf(e -> now.isAfter(e.getValue().expiresAt()));
    }
}