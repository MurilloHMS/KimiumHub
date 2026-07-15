package com.proautokimium.api.Infrastructure.repositories;

import com.proautokimium.api.domain.entities.processoSeletivo.Candidato;
import com.proautokimium.api.domain.entities.processoSeletivo.Candidatura;
import com.proautokimium.api.domain.entities.processoSeletivo.HistoricoEtapa;
import com.proautokimium.api.domain.entities.processoSeletivo.Vaga;
import com.proautokimium.api.domain.enums.processoSeletivo.Etapa;
import com.proautokimium.api.domain.enums.processoSeletivo.StatusCandidatura;
import com.proautokimium.api.domain.enums.processoSeletivo.StatusVaga;
import com.proautokimium.api.domain.valueObjects.Email;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;

import java.time.LocalDateTime;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@ActiveProfiles("test")
class HistoricoEtapaMappingTest {

    @Autowired
    private TestEntityManager entityManager;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    @DisplayName("Etapa deve ser persistida como nome e não como número")
    void ShouldPersistsStringNotOrdinal(){
        Candidatura application = persistedApplication();
        HistoricoEtapa historicoEtapa = new HistoricoEtapa(application, Etapa.TRIAGEM, Etapa.ENTREVISTA_RH, StatusCandidatura.EM_ANDAMENTO, "observacao", LocalDateTime.now());

        entityManager.persistAndFlush(historicoEtapa);

        Map<String, Object> resultado = jdbcTemplate.queryForMap(
                "SELECT etapa_anterior, etapa_nova, status FROM historico_etapas");

        String etapaAnterior = (String) resultado.get("etapa_anterior");
        String etapaNova = (String) resultado.get("etapa_nova");
        String status = (String) resultado.get("status");

        assertThat(etapaAnterior).isEqualTo("TRIAGEM");
        assertThat(etapaNova).isEqualTo("ENTREVISTA_RH");
        assertThat(status).isEqualTo("EM_ANDAMENTO");

    }

    // Helper
    private Candidatura persistedApplication(){
        Vaga vacancy = new Vaga();
        vacancy.setTitulo("teste");
        vacancy.setDescricao("teste");
        vacancy.setRequisitos("teste");
        vacancy.setBeneficios("teste");
        vacancy.setStatus(StatusVaga.PUBLICADA);
        entityManager.persist(vacancy);

        Candidato candidate = new Candidato();
        candidate.setNome("teste");
        candidate.setEmail(new Email("teste@email.com"));
        candidate.setTelefone("11975777777");
        entityManager.persist(candidate);

        Candidatura application = new Candidatura();
        application.setCandidato(candidate);
        application.setVaga(vacancy);
        return entityManager.persistAndFlush(application);
    }
}
