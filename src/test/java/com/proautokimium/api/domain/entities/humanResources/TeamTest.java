package com.proautokimium.api.domain.entities.humanResources;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.*;
import static org.junit.jupiter.api.Assertions.assertThrows;

class TeamTest {

    @Test
    @DisplayName("Não deve criar um Time se departamento for nulo")
    void shouldRejectCreateTeamWhenDepartmentIsNull(){
        assertThrows(
                IllegalArgumentException.class,
                () -> new Team("COMPRAS", null)
        );
    }
}