package com.proautokimium.api.Infrastructure.repositories.permission;

import com.proautokimium.api.domain.entities.permission.Screen;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ScreenRepository extends JpaRepository<Screen, String> {
    /** A ordem do grid de configuração: módulo, e dentro dele a do menu. */
    List<Screen> findByActiveTrueOrderByModuleAscSortOrderAsc();
}
