package com.proautokimium.api.domain.entities;

import com.proautokimium.api.Application.DTOs.partners.CustomerRequestDTO;
import com.proautokimium.api.domain.valueObjects.Email;
import jakarta.persistence.Column;
import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.Objects;

@Entity
@DiscriminatorValue("CLIENTE")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Customer extends Partner {
    @Column(name = "recebe_email")
    private boolean recebeEmail;
    @Column(name = "codigo_matriz", length = 9)
    private String codigoMatriz;
    /** Matriz do grupo: enxerga as próprias unidades no portal do cliente. */
    @Column(name = "is_matriz")
    private boolean isMatriz;

    /**
     * Matriz é quando o parceiro aponta para si mesmo, e só quando existe grupo.
     *
     * <p>É o que o ERP faz: no {@code TGFPAR}, o {@code CODPARCMATRIZ} da matriz
     * é o próprio {@code CODPARC}. E é o que o código já assumia —
     * {@code ClientAccessService.visibleUnits()} busca as unidades pelo próprio
     * código e depois filtra a própria linha do resultado.
     *
     * <p><b>O vazio não conta.</b> Sem código de matriz não há grupo, e sem
     * grupo não há matriz. Sem essa condição, as linhas legadas com código em
     * branco — duas na produção — virariam matriz de um grupo que não existe,
     * e o Java passaria a discordar do recorte que a V101 usou no backfill.
     *
     * <p>Um método só, chamado pelos dois caminhos de escrita: aqui e no
     * {@code CustomerService.UpdateCustomer}. Duplicar a regra é como os dois
     * lados começam a divergir.
     */
    public static boolean isMatriz(String codParceiro, String codigoMatriz) {
        return codigoMatriz != null
                && !codigoMatriz.isBlank()
                && Objects.equals(codParceiro, codigoMatriz);
    }

    public Customer(String systemCode, String documento, String nome, String username, Email email, boolean ativo, boolean recebeEmail, String codigoMatriz, boolean isMatriz) {
        super(systemCode, documento, nome, email, username ,ativo);
        this.recebeEmail = recebeEmail;
        this.codigoMatriz = codigoMatriz;
        this.isMatriz = isMatriz(systemCode, codigoMatriz);
    }
    public static Customer fromDTO(CustomerRequestDTO dto){
        return new Customer(
                dto.codParceiro(),
                dto.documento(),
                dto.nome(),
                dto.username(),
                new Email(dto.email()),
                dto.ativo(),
                dto.recebeEmail(),
                dto.codMatriz(),
                isMatriz(dto.codParceiro(), dto.codMatriz()));
    }
}
