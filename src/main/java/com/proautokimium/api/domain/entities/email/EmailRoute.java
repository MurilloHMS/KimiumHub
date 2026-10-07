package com.proautokimium.api.domain.entities.email;

import com.proautokimium.api.domain.entities.EmailEntity;
import com.proautokimium.api.domain.enums.email.EmailOrigin;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * De qual e-mail da empresa sai cada origem, e para onde vão as respostas.
 * Origem sem linha aqui usa o remetente padrão.
 */
@Entity
@Table(name = "email_routes")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class EmailRoute {

    @Id
    @Enumerated(EnumType.STRING)
    @Column(name = "origin", length = 40)
    private EmailOrigin origin;

    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "sender_id", nullable = false)
    private EmailEntity sender;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "reply_to_id")
    private EmailEntity replyTo;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @Column(name = "updated_by", length = 100)
    private String updatedBy;

    public EmailRoute(EmailOrigin origin, EmailEntity sender, EmailEntity replyTo, String login, LocalDateTime now) {
        this.origin = origin;
        change(sender, replyTo, login, now);
    }

    public void change(EmailEntity sender, EmailEntity replyTo, String login, LocalDateTime now) {
        this.sender = sender;
        this.replyTo = replyTo;
        this.updatedBy = login;
        this.updatedAt = now;
    }
}
