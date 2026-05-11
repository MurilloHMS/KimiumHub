package com.proautokimium.api.Infrastructure.schedulers;

import com.proautokimium.api.Infrastructure.repositories.processoSeletivo.VagaRepository;
import com.proautokimium.api.domain.entities.processoSeletivo.Vaga;
import com.proautokimium.api.domain.enums.processoSeletivo.StatusVaga;
import jakarta.transaction.Transactional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;

@Component
public class VagaScheduler {

    @Autowired
    private VagaRepository repository;

    private final Logger logger = LoggerFactory.getLogger(VagaScheduler.class);


    @Transactional
    @Scheduled(cron="0 0 */3 * * *")
    public void updateVaga(){
        List<Vaga> vagasVencidas = repository.findAllByDataEncerramentoBeforeAndStatus(LocalDateTime.now(), StatusVaga.PUBLICADA);

        if(vagasVencidas == null || vagasVencidas.isEmpty())
            return;

        logger.info("Encontradas {} vagas com a data vencida", vagasVencidas.size());
        for(Vaga vaga : vagasVencidas){
            try{
                vaga.encerrar();
                logger.info("Vaga {} encerrada com sucesso", vaga.getId());
            }catch (Exception e){
                logger.error("Erro ao encerrar vaga {}", vaga.getId(), e);
            }
        }
        logger.info("Vagas encerradas com sucesso!");
    }
}
