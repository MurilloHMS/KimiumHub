package com.proautokimium.api.domain.entities.processoSeletivo;

import com.proautokimium.api.domain.enums.processoSeletivo.Etapa;
import com.proautokimium.api.domain.enums.processoSeletivo.StatusCandidatura;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import static org.assertj.core.api.Assertions.*;

class CandidaturaTest {

    @Test
    @DisplayName("Deve iniciar uma candidatura com status Em_Andamento e etapa Triagem")
    void shouldInitiateApplication(){
        Candidatura candidatura = new Candidatura();
        candidatura.iniciar();

        assertThat(candidatura.getEtapaAtual()).isEqualTo(Etapa.TRIAGEM);
        assertThat(candidatura.getStatus()).isEqualTo(StatusCandidatura.EM_ANDAMENTO);
        assertThat(candidatura.getCriadoEm()).isNotNull();
    }

    @Test
    @DisplayName("Deve avançar etapa em ordem até o fim — sem ENTREVISTA_GESTOR, adiada para 2027 por decisão de negócio")
    void shouldNextStepsOnOrder(){
        Candidatura candidatura = new Candidatura();
        candidatura.iniciar();

        candidatura.avancarEtapa();
        assertThat(candidatura.getEtapaAtual()).isEqualTo(Etapa.ENTREVISTA_RH);
        assertThat(candidatura.getAtualizadoEm()).isNotNull();

        candidatura.avancarEtapa();
        assertThat(candidatura.getEtapaAtual()).isEqualTo(Etapa.PROPOSTA);
        assertThat(candidatura.getAtualizadoEm()).isNotNull();


        candidatura.avancarEtapa();
        assertThat(candidatura.getEtapaAtual()).isEqualTo(Etapa.CONTRATADO);
        assertThat(candidatura.getStatus()).isEqualTo(StatusCandidatura.APROVADO);
        assertThat(candidatura.getAtualizadoEm()).isNotNull();

    }

    @ParameterizedTest
    @EnumSource(value = StatusCandidatura.class, names = {"REPROVADO", "ENCERRADO"})
    @DisplayName("Deve rejeitar avançar etapa se status for reprovado ou encerrado")
    void shouldRejectNextStepIfStatusIsReprovedOrClosed(StatusCandidatura status){
        Candidatura candidatura = new Candidatura();
        candidatura.setStatus(status);

        assertThatThrownBy(candidatura::avancarEtapa).isInstanceOf(IllegalStateException.class);
    }

    @Test
    @DisplayName("Deve rejeitar reprovar uma candidatura, se estiver com status aprovado e na etapa contratado")
    void shouldRejectReprovedIfStepIsContracted(){
        Candidatura candidatura = new Candidatura();
        candidatura.setStatus(StatusCandidatura.APROVADO);
        candidatura.setEtapaAtual(Etapa.CONTRATADO);

        assertThatThrownBy(candidatura::reprovar).isInstanceOf(IllegalStateException.class);
        assertThat(candidatura.getStatus()).isEqualTo(StatusCandidatura.APROVADO);
    }

    @Test
    @DisplayName("Deve rejeitar avancar etapa caso esteja contratado")
    void shouldRejectNextStepIfContracted(){
        Candidatura candidatura = new Candidatura();
        candidatura.setEtapaAtual(Etapa.CONTRATADO);

        assertThatThrownBy(candidatura::avancarEtapa).isInstanceOf(IllegalStateException.class);
    }

    @ParameterizedTest
    @EnumSource(value = StatusCandidatura.class, names = {"APROVADO", "EM_ANDAMENTO"})
    @DisplayName("Deve rejeitar iniciar uma candidatura em andamento ou aprovada")
    void shouldRejectInitiateApplicationIfStatusInProgress(StatusCandidatura status){
        Candidatura candidatura = new Candidatura();
        candidatura.setStatus(status);

        assertThatThrownBy(candidatura::iniciar).isInstanceOf(IllegalStateException.class);
    }

    @Test
    @DisplayName("Deve encerrar uma candidatura em andamento")
    void shouldCloseApplication(){
        Candidatura candidatura = new Candidatura();
        candidatura.encerrar();

        assertThat(candidatura.getStatus()).isEqualTo(StatusCandidatura.ENCERRADO);
        assertThat(candidatura.getAtualizadoEm()).isNotNull();
    }

    @Test
    @DisplayName("Não deve encerrar uma candidatura caso esteja como CONTRATADO ou APROVADO")
    void shouldRejectCloseIfContracted(){
        Candidatura candidatura = new Candidatura();
        candidatura.setStatus(StatusCandidatura.APROVADO);

        assertThatThrownBy(candidatura::encerrar).isInstanceOf(IllegalStateException.class);
        assertThat(candidatura.getStatus()).isEqualTo(StatusCandidatura.APROVADO);

        candidatura.setStatus(StatusCandidatura.EM_ANDAMENTO);
        candidatura.setEtapaAtual(Etapa.CONTRATADO);
        assertThatThrownBy(candidatura::encerrar).isInstanceOf(IllegalStateException.class);
        assertThat(candidatura.getStatus()).isEqualTo(StatusCandidatura.EM_ANDAMENTO);
        assertThat(candidatura.getEtapaAtual()).isEqualTo(Etapa.CONTRATADO);


    }

    @Test
    @DisplayName("Deve rejeitar encerrar candidatura caso já esteja encerrada")
    void shouldRejectCloseIfAlreadyClosed(){
        Candidatura candidatura = new Candidatura();
        candidatura.setStatus(StatusCandidatura.ENCERRADO);

        assertThatThrownBy(candidatura::encerrar).isInstanceOf(IllegalStateException.class);
    }

    @Test
    @DisplayName("Deve rejeitar avançar uma candidatura que esteja em entrevista com o gestor")
    void shouldRejectNextStepIfApplicationIsEntrevistaGestor(){
        // Atualmente a etapa de entrevista com o Gestor está adiada o desenvolvimento, então não poderá ser aprovado avançar etapa caso esteja nesta etapa
        Candidatura candidatura = new Candidatura();
        candidatura.setEtapaAtual(Etapa.ENTREVISTA_GESTOR);

        assertThatThrownBy(candidatura::avancarEtapa).isInstanceOf(IllegalStateException.class);
    }
}