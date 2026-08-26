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
     * Ordena por `createdAt`, e **não** por `movementDate`. A diferença não é
     * estilo: `movement_date` é `date`, sem hora, então dois lançamentos do
     * mesmo dia empatavam e o desempate caía no id — um UUID aleatório. O
     * estoque atual era sorteado entre os movimentos do dia. Ver a V83.
     *
     * `movementDate` continua sendo quando a movimentação aconteceu, e serve
     * para relatório. Estoque atual é o último **registrado**: uma entrada
     * lançada com data retroativa não pode mudar o estoque de hoje.
     *
     * O id fica de desempate para as linhas antigas, que a V83 nivelou em
     * meia-noite — arbitrário, mas ao menos estável entre consultas.
     */
    Optional<MovementInventory> findTopByProductOrderByCreatedAtDescIdDesc(ProductInventory product);
}
