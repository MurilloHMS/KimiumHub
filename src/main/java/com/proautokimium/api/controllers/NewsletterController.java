package com.proautokimium.api.controllers;

import com.proautokimium.api.Infrastructure.services.email.NewsletterService;
import com.proautokimium.api.domain.models.Newsletter;
import jakarta.mail.MessagingException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.UnsupportedEncodingException;

@RestController
@RequestMapping("api/newsletter")
public class NewsletterController {
    @Autowired
    private NewsletterService newsletterService;

    @PostMapping("send")
    public ResponseEntity<Object> sendNewsletter(@RequestBody Newsletter newsletter) throws MessagingException, UnsupportedEncodingException {
        newsletterService.sendMailWithInline(newsletter);

        return ResponseEntity.ok().build();
    }
}
