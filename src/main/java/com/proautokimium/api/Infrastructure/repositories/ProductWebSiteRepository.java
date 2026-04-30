package com.proautokimium.api.Infrastructure.repositories;

import com.proautokimium.api.domain.entities.ProductWebsite;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.UUID;

public interface ProductWebSiteRepository extends JpaRepository<ProductWebsite, UUID> {

    @Query(value = """
        SELECT * FROM products WHERE cores @> CAST(:cores AS jsonb)
    """, nativeQuery = true)
    List<ProductWebsite> findByCores(@Param("cores") String coresJson);

    @Query(value = """
        SELECT * FROM products WHERE cores ?| ARRAY[:cores]
    """, nativeQuery = true)
    List<ProductWebsite> findByAnyCor(@Param("cores") String[] cores);

    List<ProductWebsite> findAllByActive(boolean value);
}
