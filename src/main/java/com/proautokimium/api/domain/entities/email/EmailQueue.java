package com.proautokimium.api.domain.entities.email;

import com.proautokimium.api.domain.enums.EmailStatus;
import jakarta.mail.internet.MimeMessage;
import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.Arrays;

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

}