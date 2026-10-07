package com.proautokimium.api.domain.entities.email;

import com.proautokimium.api.domain.enums.EmailStatus;
import com.proautokimium.api.domain.enums.email.EmailOrigin;
import jakarta.mail.internet.MimeMessage;
import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@Entity
@Table(name = "email_queue")
public class EmailQueue extends com.proautokimium.api.domain.abstractions.Entity{
    @Column(name = "to_email", nullable = false)
    private String toEmail;
    @Column(name = "reply_to")
    private String replyTo;
    @Column(name = "from_email", nullable = false)
    private String fromEmail;
    @Column(name = "subject", nullable = false)
    private String subject;
    @Column(name = "body" ,columnDefinition = "TEXT", nullable = false)
    private String body;
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private EmailStatus status;
    @Column(name = "attempts")
    private int attempts;
    @Column(name = "created_at")
    private LocalDateTime createdAt;
    @Column(name = "sent_at")
    private LocalDateTime sentAt;
    @Enumerated(EnumType.STRING)
    @Column(name = "origin", length = 40)
    private EmailOrigin origin;
    @Column(name = "from_name", length = 120)
    private String fromName;
    @Column(name = "last_error", columnDefinition = "TEXT")
    private String lastError;
    @Column(name = "last_attempt_at")
    private LocalDateTime lastAttemptAt;
    @OneToMany(mappedBy = "email", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<EmailAttachment> attachments = new ArrayList<>();

    /** Quantas tentativas o agendador faz antes de desistir. */
    public static final int MAX_ATTEMPTS = 5;
    /** O erro do SMTP pode trazer a pilha inteira; o começo é o que diz o motivo. */
    static final int MAX_ERROR_LENGTH = 2000;

    // Constructors

    public EmailQueue(){};

    public EmailQueue(String toEmail, String replyTo, String fromEmail, String subject, String body, EmailStatus status, int attempts) {
        this.toEmail = toEmail;
        this.replyTo = replyTo;
        this.fromEmail = fromEmail;
        this.subject = subject;
        this.body = body;
        this.status = status;
        this.attempts = attempts;
        this.createdAt = LocalDateTime.now();
    }

    public EmailQueue(String toEmail, String fromEmail, String subject, String body) {
        this.toEmail = toEmail;
        this.fromEmail = fromEmail;
        this.subject = subject;
        this.body = body;
        this.createdAt = LocalDateTime.now();
    }

    // Getters and Setters
    public String getToEmail() {
        return toEmail;
    }

    public String getReplyTo() {
        return replyTo;
    }

    public String getFromEmail() {
        return fromEmail;
    }

    public String getSubject() {
        return subject;
    }

    public String getBody() {
        return body;
    }

    public EmailStatus getStatus() {
        return status;
    }

    public void setStatus(EmailStatus status) {
        this.status = status;
    }

    public int getAttempts() {
        return attempts;
    }

    // Methods
    public void retrySentEmail(){
        this.status = EmailStatus.PENDING;

        if(this.attempts <= 5){
            this.attempts++;
        }else{
            this.status = EmailStatus.FAILED;
        }
    }

    public void markEmailSent(){
        this.status = EmailStatus.SENT;
        this.sentAt = LocalDateTime.now();
    }

    public void markSchedule(){
        this.status = EmailStatus.SCHEDULED;
    }

    /**
     * Um e-mail novo de uma origem. O remetente ainda não está aqui: quem decide
     * é a configuração (EmailSenderResolver), na hora de enfileirar.
     */
    public static EmailQueue of(EmailOrigin origin, String to, String subject, String body, LocalDateTime now) {
        EmailQueue email = new EmailQueue();
        email.origin = origin;
        email.toEmail = to;
        email.subject = subject;
        email.body = body;
        email.attempts = 0;
        email.createdAt = now;
        return email;
    }

    /** O remetente do momento: mudar a configuração depois não reescreve este e-mail. */
    public void assignSender(String fromEmail, String fromName, String replyTo) {
        this.fromEmail = fromEmail;
        this.fromName = fromName;
        this.replyTo = replyTo;
    }

    public void addAttachment(String filename, String contentType, String storagePath, long sizeBytes, LocalDateTime now) {
        attachments.add(new EmailAttachment(this, filename, contentType, storagePath, sizeBytes, now));
    }

    /** Saiu: o SMTP aceitou. */
    public void markSent(LocalDateTime now) {
        this.status = EmailStatus.SENT;
        this.sentAt = now;
        this.lastAttemptAt = now;
        this.attempts++;
        this.lastError = null;
    }

    /**
     * Não saiu: guarda o motivo. Volta para a fila até {@link #MAX_ATTEMPTS}
     * tentativas; depois fica FAILED, esperando alguém reenviar.
     */
    public void recordFailure(String error, LocalDateTime now) {
        this.attempts++;
        this.lastAttemptAt = now;
        this.lastError = error == null ? null
                : (error.length() > MAX_ERROR_LENGTH ? error.substring(0, MAX_ERROR_LENGTH) : error);
        this.status = attempts >= MAX_ATTEMPTS ? EmailStatus.FAILED : EmailStatus.PENDING;
    }

    /** Envio na hora (código de acesso): uma tentativa só, porque o código expira antes da próxima. */
    public void recordImmediateFailure(String error, LocalDateTime now) {
        recordFailure(error, now);
        this.status = EmailStatus.FAILED;
    }

    /**
     * Pode voltar para a fila? Só o que falhou, e nunca o que leva código ou
     * link de acesso: o código da redefinição e do primeiro acesso expira em
     * minutos (e, se o envio falhou na hora, nem chegou a ser gravado), e os
     * links têm prazo. Reenviar entregaria um acesso morto; a pessoa pede outro.
     */
    public boolean canBeResent() {
        return status == EmailStatus.FAILED && (origin == null || !origin.isSensitive());
    }

    /** Reenviar: só o que falhou volta para a fila, com as tentativas zeradas. */
    public void requeue() {
        if (status != EmailStatus.FAILED) {
            throw new com.proautokimium.api.domain.exceptions.humanResources.InvalidStatusTransitionException(
                    "Só um e-mail que falhou pode ser reenviado.");
        }
        if (!canBeResent()) {
            throw new com.proautokimium.api.domain.exceptions.humanResources.InvalidStatusTransitionException(
                    "Este e-mail leva um código de acesso com prazo: em vez de reenviar, peça um novo.");
        }
        this.status = EmailStatus.PENDING;
        this.attempts = 0;
        this.lastError = null;
    }

    public EmailOrigin getOrigin() { return origin; }
    public String getFromName() { return fromName; }
    public String getLastError() { return lastError; }
    public LocalDateTime getLastAttemptAt() { return lastAttemptAt; }
    public List<EmailAttachment> getAttachments() { return attachments; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public LocalDateTime getSentAt() { return sentAt; }

}