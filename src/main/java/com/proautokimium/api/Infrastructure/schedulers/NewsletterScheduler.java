package com.proautokimium.api.Infrastructure.schedulers;

import com.proautokimium.api.Infrastructure.services.email.newsletter.NewsletterOrchestratorService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class NewsletterScheduler {

    @Autowired
    private NewsletterOrchestratorService service;

    private final Logger logger = LoggerFactory.getLogger(NewsletterScheduler.class);

    @Scheduled(cron = "0 * * 5-10 * 1-5")
    public void sendEmailsScheduled(){
        service.executeMonthlyNewsletter();
    }
}
