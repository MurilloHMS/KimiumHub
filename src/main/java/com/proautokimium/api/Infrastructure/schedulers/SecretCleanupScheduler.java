package com.proautokimium.api.Infrastructure.schedulers;

import com.proautokimium.api.Infrastructure.repositories.PublicSecretRepository;
import com.proautokimium.api.Infrastructure.repositories.RegistroPontoRepository;
import jakarta.transaction.Transactional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

@Component
public class SecretCleanupScheduler {
    private final PublicSecretRepository repository;

    private final Logger logger = LoggerFactory.getLogger(SecretCleanupScheduler.class);

    public SecretCleanupScheduler(PublicSecretRepository repository) {
        this.repository = repository;
    }

    @Scheduled(cron = "0 * * * * *")
    @Transactional
    public void deleteExpired(){
        int n = repository.deleteExpired(LocalDateTime.now());
        if(n > 0) logger.info("[OTS] {} expired secrets removed", n);
    }

}
