package com.proautokimium.api.Infrastructure.repositories.prostock;

import com.proautokimium.api.domain.entities.prostock.ProductInventory;
import com.proautokimium.api.domain.entities.prostock.machine.MachineRegister;
import com.proautokimium.api.domain.enums.MachineStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.UUID;

public interface RegisterRepository extends JpaRepository<MachineRegister, UUID> {
    List<MachineRegister> findAllByMachine(ProductInventory machine);

    /**
     * Quantas programações de cada máquina estão em estoque.
     *
     * Agrupado no banco, e não contado em Java depois de trazer tudo: a
     * programação passa de duzentas linhas e o Hub só precisa do número.
     *
     * Devolve `[machine_id, total]`. Máquina sem nenhuma linha em estoque não
     * aparece — quem chama trata como zero.
     */
    @Query("""
        SELECT r.machine.id, COUNT(r)
        FROM MachineRegister r
        WHERE r.status IN :statuses
        GROUP BY r.machine.id
        """)
    List<Object[]> countInStockByMachine(@Param("statuses") Collection<MachineStatus> statuses);
}
