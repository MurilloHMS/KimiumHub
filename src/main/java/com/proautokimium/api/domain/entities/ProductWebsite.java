package com.proautokimium.api.domain.entities;

import jakarta.persistence.Column;
import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.io.Serial;
import java.util.List;

@Entity
@DiscriminatorValue("WEBSITE")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ProductWebsite extends ProductEntity{
    @Serial
    private static final long serialVersionUID = 1L;

    @Column(name = "path_imagem", length = 200)
    private String imagem;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "jsonb")
    private List<String> cores;
    @Column(name = "finalidade", length = 500)
    private String finalidade;
    @Column(name = "diluicao", length = 100)
    private String diluicao;
    @Column(name = "descricao", length = 1000)
    private String descricao;
}
