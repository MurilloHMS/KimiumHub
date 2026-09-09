package com.proautokimium.api.domain.entities;

import com.proautokimium.api.domain.abstractions.Entity;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Duration;
import java.time.LocalTime;

/**
 * Uma ordem de serviço do mês, guardada linha a linha.
 *
 * **A tabela de cliente é agregada e a correção é por OS** — por isso esta
 * existe. Sem ela, corrigir uma hora significaria editar o valor somado à mão,
 * e o próximo cálculo apagaria a correção sem avisar.
 *
 * O texto do ERP fica guardado como veio. Ele é a única prova do que foi
 * anotado: {@code "5:00 horas"} tanto pode ser cinco da manhã quanto cinco horas
 * de trabalho, e só quem escreveu sabe.
 */
@jakarta.persistence.Entity
@Table(
        name = "newsletter_previa_os",
        uniqueConstraints = @UniqueConstraint(name = "uk_newsletter_previa_os", columnNames = {"previa_id", "numero_os"})
)
@Getter
@Setter
@NoArgsConstructor
public class NewsletterPreviaOs extends Entity {

    /** Cobrança por hora da assistência, como estava na consulta original. */
    public static final double VALOR_DA_HORA = 150d;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "previa_id", nullable = false)
    private NewsletterPrevia previa;

    @Column(name = "codigo_cliente", nullable = false, length = 50)
    private String codigoCliente;

    @Column(name = "numero_os", nullable = false)
    private int numeroOs;

    @Column(name = "mau_uso", nullable = false)
    private boolean mauUso;

    /** Como está no ERP, sem interpretar. `13:00`, `14h20`, `1603`, vazio. */
    @Column(name = "hora_inicio_texto", length = 100)
    private String horaInicioTexto;

    @Column(name = "hora_fim_texto", length = 100)
    private String horaFimTexto;

    /** O que deu para ler — do texto acima, ou digitado na revisão. */
    @Column(name = "hora_inicio")
    private LocalTime horaInicio;

    @Column(name = "hora_fim")
    private LocalTime horaFim;

    /**
     * Ficou pendente quando alguma das duas pontas não deu para ler.
     *
     * Antes desta tela essas OS sumiam em silêncio: o `TRY_CAST` do SQL devolvia
     * `NULL` e o `WHERE hora_inicio IS NOT NULL` descartava a linha. Foram 82
     * das 350 de junho — quase um quarto do valor de horas.
     */
    public boolean estaPendente() {
        return horaInicio == null || horaFim == null;
    }

    /**
     * As horas desta OS.
     *
     * **Fim antes do início devolve zero**, e não um número negativo: a OS que
     * atravessa a meia-noite existe, mas subtrair errado produziria uma hora
     * negativa que some do total de outro atendimento — um erro que ninguém vê.
     * Zero deixa a OS visível como "sem valor" em vez de corromper a soma.
     */
    public double horas() {
        if (estaPendente()) {
            return 0d;
        }

        long minutos = Duration.between(horaInicio, horaFim).toMinutes();
        return minutos <= 0 ? 0d : minutos / 60d;
    }

    public double valorCobrado() {
        return horas() * VALOR_DA_HORA;
    }
}
