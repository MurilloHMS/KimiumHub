package com.proautokimium.api.domain.entities;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;

@Entity
@Table(name = "registros_ponto", indexes = {
        @Index(name = "idx_employee_data", columnList = "id, data"),
        @Index(name = "idx_emplyee_mesano", columnList = "id, mes_ano")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class RegistroPonto extends com.proautokimium.api.domain.abstractions.Entity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "employee_id", nullable = false)
    private Employee employee;

    @Column(name = "data", nullable = false)
    private LocalDate data;

    @Column(name = "entrada")
    private LocalTime entrada;

    @Column(name = "almoco_saida")
    private LocalTime almocoSaida;

    @Column(name = "almoco_retorno")
    private LocalTime almocoRetorno;

    @Column(name = "saida")
    private LocalTime saida;

    @Column(name = "mes_ano", length = 7, nullable = false)
    private String mesAno;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    @PreUpdate
    private void setMesAno(){
        if(data  != null){
            this.mesAno = data.format(DateTimeFormatter.ofPattern("yyyy-MM"));
        }
    }

    public String calcularHorasTrabalhadas(){
        if(entrada == null || saida == null){
            return "--:--";
        }

        Duration duracao = Duration.between(entrada, saida);

        if(almocoSaida != null && almocoRetorno != null){
            Duration almoco = Duration.between(almocoSaida, almocoRetorno);
            duracao = duracao.minus(almoco);
        }

        long horas = duracao.toHours();
        long minutos = duracao.toMinutes() % 60;

        return String.format("%02d:%02d", horas, minutos);
    }
}
