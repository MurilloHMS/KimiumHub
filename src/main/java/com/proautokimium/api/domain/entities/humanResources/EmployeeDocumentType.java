package com.proautokimium.api.domain.entities.humanResources;

import com.proautokimium.api.domain.exceptions.humanResources.InvalidRequestDataException;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

/**
 * Um tipo de documento — ASO, NR-35, contrato… — com os seus avisos.
 *
 * Os dias de aviso e os responsáveis moram AQUI, e não num lugar só para todos:
 * ASO e contrato têm prazos e donos diferentes (decisão dele, 2026-09-29).
 *
 * {@code Set} e não {@code List} nas duas coleções: o Hibernate não busca duas
 * listas EAGER na mesma entidade (MultipleBagFetchException), e a chave
 * primária das tabelas já proíbe repetição.
 */
@Entity
@Table(name = "employee_document_types")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class EmployeeDocumentType extends com.proautokimium.api.domain.abstractions.Entity{

    /** Sem dias configurados, "Vence em breve" comeca 30 dias antes. */
    public static final int DEFAULT_WARNING_DAYS = 30;

    @Column(name = "name", length = 100, nullable = false, unique = true)
    private String name;

    @Column(name = "notify_on_expiry", nullable = false)
    private boolean notifyOnExpiry = true;

    @Column(name = "active", nullable = false)
    private boolean active = true;

    @Column(name =  "created_at", nullable = false)
    private LocalDateTime createdAt;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "employee_document_type_alert_days", joinColumns = @JoinColumn(name = "type_id"))
    @Column(name = "days_before", nullable = false)
    private Set<Integer> alertDaysBefore = new HashSet<>();

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "employee_document_type_recipients", joinColumns = @JoinColumn(name = "type_id"))
    @Column(name = "employee_id", nullable = false)
    private Set<UUID> recipientEmployeeIds = new HashSet<>();

    public static EmployeeDocumentType create(String name, LocalDateTime now){
        EmployeeDocumentType type = new EmployeeDocumentType();
        type.rename(name);
        type.createdAt = now;
        return type;
    }

    public void rename(String name){
        if(name == null || name.isBlank()){
            throw new InvalidRequestDataException("Informe o nome do tipo de documento.");
        }
        this.name = name.trim();
    }

    /**
     * Troca os avisos de uma vez. Zero e negativo são recusados: "0 dias antes"
     * é o próprio vencimento, que tem chave própria ({@code notifyOnExpiry}).
     */
    public void configureAlerts(Collection<Integer> daysBefore, boolean notifyOnExpiry, Collection<UUID> recipientEmployeeIds){
        if(daysBefore != null && daysBefore.stream().anyMatch(day -> day == null || day <= 0)){
            throw new InvalidRequestDataException("Os dias de aviso precisam ser maiores que zero");
        }

        this.alertDaysBefore.clear();
        if(daysBefore != null) this.alertDaysBefore.addAll(daysBefore);

        this.recipientEmployeeIds.clear();
        if(recipientEmployeeIds != null) this.recipientEmployeeIds.addAll(recipientEmployeeIds);

        this.notifyOnExpiry = notifyOnExpiry;
    }

    public void deactivate(){
        this.active = false;
    }

    public void activate(){
        this.active = true;
    }

    /** Quantos dias antes do vencimento o documento passa a "vence em breve". */
    public int warningWindowDays(){
        return alertDaysBefore.stream().max(Integer::compare).orElse(DEFAULT_WARNING_DAYS);
    }
}
