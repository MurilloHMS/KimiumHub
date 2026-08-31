package com.proautokimium.api.Infrastructure.abstractions.storage;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.*;

class FileStorageTest {

    // Uma subclasse mínima, só para poder chamar o método.
    // Os dois caminhos não importam: o teste não grava nada em disco.
    private static class StorageDeTeste extends FileStorage {
        @Override protected String getStoragePath() { return "/tmp"; }
        @Override protected String getReturnPath()  { return "/tmp/"; }
    }

    @Test
    @DisplayName("O nome mantém a extensão original")
    void shouldKeepTheOriginalExtension(){
        StorageDeTeste storage = new StorageDeTeste();

        String filename = storage.buildFileName("arquivo.png", "teste");

        assertThat(filename).endsWith(".png");

    }
}