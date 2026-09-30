package com.proautokimium.api.domain.entities.auth;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Uma digital cadastrada num aparelho. Guarda só a chave pública: serve para
 * conferir assinaturas e para mais nada.
 *
 * <p>{@code userId} como texto, e não {@code @ManyToOne User}, como o
 * {@link RefreshToken}: aqui só o id interessa, e o usuário inteiro é buscado
 * uma vez, no login, quando precisa.
 */
@Entity
@Table(name = "webauthn_credentials")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class WebAuthnCredential extends com.proautokimium.api.domain.abstractions.Entity {

    @Column(name = "user_id", nullable = false)
    private String userId;

    @Column(name = "credential_id", nullable = false, unique = true, length = 1400)
    private String credentialId;

    @Column(name = "attested_credential_data", nullable = false)
    private byte[] attestedCredentialData;

    @Column(name = "sign_count", nullable = false)
    private long signCount;

    @Column(name = "uv_initialized", nullable = false)
    private boolean uvInitialized;

    @Column(name = "backup_eligible", nullable = false)
    private boolean backupEligible;

    @Column(name = "backup_state", nullable = false)
    private boolean backupState;

    @Column(name = "transports", length = 100)
    private String transports;

    @Column(name = "device_label", nullable = false, length = 120)
    private String deviceLabel;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "last_used_at")
    private LocalDateTime lastUsedAt;

    private WebAuthnCredential(String userId, String credentialId, byte[] attestedCredentialData, long signCount,
                               boolean uvInitialized, boolean backupEligible, boolean backupState,
                               String transports, String deviceLabel, LocalDateTime now) {
        this.userId = userId;
        this.credentialId = credentialId;
        this.attestedCredentialData = attestedCredentialData;
        this.signCount = signCount;
        this.uvInitialized = uvInitialized;
        this.backupEligible = backupEligible;
        this.backupState = backupState;
        this.transports = transports;
        this.deviceLabel = deviceLabel;
        this.createdAt = now;
    }

    /** Uma digital recém-conferida pela webauthn4j, pronta para gravar. */
    public static WebAuthnCredential register(String userId, String credentialId, byte[] attestedCredentialData,
                                              long signCount, boolean uvInitialized, boolean backupEligible,
                                              boolean backupState, String transports, String deviceLabel,
                                              LocalDateTime now) {
        return new WebAuthnCredential(userId, credentialId, attestedCredentialData, signCount,
                uvInitialized, backupEligible, backupState, transports, deviceLabel, now);
    }

    /**
     * Depois de um login aprovado: o contador novo (é com ele que a próxima
     * verificação detecta cópia) e se a credencial passou a estar sincronizada.
     * {@code backupEligible} não muda: pela norma, é fixo da credencial.
     */
    public void recordUse(long newSignCount, boolean newBackupState, LocalDateTime now) {
        this.signCount = newSignCount;
        this.backupState = newBackupState;
        this.lastUsedAt = now;
    }

    public boolean belongsTo(String userId) {
        return this.userId.equals(userId);
    }
}
