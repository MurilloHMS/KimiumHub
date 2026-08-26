package com.proautokimium.api.Infrastructure.repositories.prostock;

import com.proautokimium.api.domain.entities.prostock.MovementInventory;
import com.proautokimium.api.domain.entities.prostock.ProductInventory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ProductMovementRepository extends JpaRepository<MovementInventory, UUID> {
    @Query(value = "SELECT * FROM products_movements WHERE product_id = :id", nativeQuery = true)
    List<MovementInventory> findMovementByProductId(@Param("id") UUID id);

    /**
     * O último movimento é o estoque atual: a base guarda o valor absoluto
     * resultante, não a diferença.
     *
     * Empate de data resolve pelo id, só para o resultado ser determinístico.
     * Dois lançamentos no mesmo instante já são ambíguos por natureza, e a
     * ordenação não conserta isso — só evita que a resposta mude a cada consulta.
     */
    Optional<MovementInventory> findTopByProductOrderByMovementDateDescIdDesc(ProductInventory product);
}
