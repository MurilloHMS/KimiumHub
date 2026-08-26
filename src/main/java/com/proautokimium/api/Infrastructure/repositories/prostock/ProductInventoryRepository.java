package com.proautokimium.api.Infrastructure.repositories.prostock;

import com.proautokimium.api.domain.entities.prostock.ProductInventory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ProductInventoryRepository extends JpaRepository<ProductInventory, UUID> {
    /**
     * `Optional` e não a entidade crua.
     *
     * Com o retorno cru dá para esquecer o nulo, e foi o que aconteceu: três
     * dos quatro chamadores checavam, e `findAllMovementsByProduct` chamava
     * `.getId()` direto. Aqui o compilador cobra.
     */
    Optional<ProductInventory> findBySystemCode(String systemCode);
    List<ProductInventory> findBySystemCodeIn(List<String> systemCode);

    /** Os produtos que também são máquina — o que antes era `type='MACHINE'`. */
    List<ProductInventory> findByIsMachineTrue();

    /**
     * Produtos cujo **último** movimento ficou abaixo do mínimo.
     *
     * Ordenava por `movementDate`, que é `date` e não tem hora: o
     * `MAX(movementDate)` casava com TODOS os lançamentos do dia, não com o
     * último. Um produto que caiu abaixo do mínimo de manhã e foi reposto à
     * tarde continuava no alerta — e ainda vinha repetido na lista, uma vez por
     * lançamento do dia. Ver a V83.
     */
    @Query("""
        SELECT p
        FROM ProductInventory p
        JOIN p.movements m
        WHERE m.createdAt = (
            SELECT MAX(m2.createdAt)
            FROM MovementInventory m2
            WHERE m2.product = p
        )
        AND m.quantity < p.minimumStock
    """)
    List<ProductInventory> findProductByMovementBelowMinimum();


}
