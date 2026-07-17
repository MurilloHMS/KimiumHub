package com.proautokimium.api.domain.entities.processoSeletivo;

import com.proautokimium.api.domain.enums.processoSeletivo.StatusVaga;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import static org.assertj.core.api.Assertions.*;

class VagaTest {

    @ParameterizedTest
    @EnumSource(value = StatusVaga.class, mode = EnumSource.Mode.EXCLUDE, names = {"PUBLICADA"})
    @DisplayName("Não deve encerrar vaga se status for diferente de publicada e deve manter o status.")
    void shouldRejectEncerrarIfStatusNotPublished(StatusVaga status){
        Vaga vaga = new Vaga();
        vaga.setStatus(status);

        assertThatThrownBy(vaga::encerrar).isInstanceOf(IllegalStateException.class);
        assertThat(vaga.getStatus()).isEqualTo(status);
    }

    @ParameterizedTest
    @EnumSource(value = StatusVaga.class, mode = EnumSource.Mode.EXCLUDE, names = {"RASCUNHO"})
    @DisplayName("Não deve publicar vaga se status for diferente de rascunho e deve manter o status")
    void shouldRejectPublishIfStatusNotDraft(StatusVaga status){
        Vaga vaga = new Vaga();
        vaga.setStatus(status);

        assertThatThrownBy(vaga::publicar).isInstanceOf(IllegalStateException.class);
        assertThat(vaga.getStatus()).isEqualTo(status);
    }

    @ParameterizedTest
    @EnumSource(value = StatusVaga.class, mode = EnumSource.Mode.EXCLUDE, names = {"ENCERRADA"})
    @DisplayName("Não deve arquivar vaga se status for diferente de encerrada e deve manter o status")
    void shouldRejectArchiveIfStatusNotClosed(StatusVaga status){
        Vaga vaga = new Vaga();
        vaga.setStatus(status);

        assertThatThrownBy(vaga::arquivar).isInstanceOf(IllegalStateException.class);
        assertThat(vaga.getStatus()).isEqualTo(status);
    }

    @ParameterizedTest
    @EnumSource(value = StatusVaga.class, mode = EnumSource.Mode.EXCLUDE, names = {"ARQUIVADA"})
    @DisplayName("Não deve rascunhar vaga se status for diferente de arquivada e deve manter o status")
    void shouldRejectDraftIfStatusNotArchived(StatusVaga status){
        Vaga vaga = new Vaga();
        vaga.setStatus(status);

        assertThatThrownBy(vaga::rascunho).isInstanceOf(IllegalStateException.class);
        assertThat(vaga.getStatus()).isEqualTo(status);
    }

    @Test
    @DisplayName("Deve publicar vaga e alterar o status para publicada")
    void shouldPublishVagaAndSetStatusToPublish(){
        Vaga vaga = new Vaga();
        vaga.setStatus(StatusVaga.RASCUNHO);

        vaga.publicar();
        assertThat(vaga.getStatus()).isEqualTo(StatusVaga.PUBLICADA);
    }

    @Test
    @DisplayName("Deve encerrar vaga e alterar o status para encerrada")
    void shouldCloseVagaAndSetStatusToClosed(){
        Vaga vaga = new Vaga();
        vaga.setStatus(StatusVaga.PUBLICADA);

        vaga.encerrar();
        assertThat(vaga.getStatus()).isEqualTo(StatusVaga.ENCERRADA);
    }

    @Test
    @DisplayName("Deve arquivar vaga e alterar o status para arquivada")
    void shouldArchiveVagaAndSetStatusToArchived(){
        Vaga vaga = new Vaga();
        vaga.setStatus(StatusVaga.ENCERRADA);

        vaga.arquivar();
        assertThat(vaga.getStatus()).isEqualTo(StatusVaga.ARQUIVADA);
    }

    @Test
    @DisplayName("Deve rascunhar vaga e alterar o status para rascunho")
    void shouldDraftVagaAndSetStatusToDraft(){
        Vaga vaga = new Vaga();
        vaga.setStatus(StatusVaga.ARQUIVADA);

        vaga.rascunho();
        assertThat(vaga.getStatus()).isEqualTo(StatusVaga.RASCUNHO);
    }
}