package com.proautokimium.api.Infrastructure.repositories;

import com.proautokimium.api.domain.entities.Customer;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface CustomerRepository extends JpaRepository<Customer, UUID> {
    Optional<Customer> findByCodParceiro(String codParceiro);
	List<Customer> findByCodParceiroIn(List<String> partnersCode);
    @Query("""
        SELECT c FROM Customer c
         WHERE function('regexp_replace', c.documento, '[^0-9]', '', 'g') = :digits
    """)
    Optional<Customer> findByCnpjDigits(@Param("digits") String digits);

    /** Unidades de um grupo: filhas apontam para o cod_parceiro da matriz. */
    List<Customer> findByCodigoMatriz(String codigoMatriz);
}
