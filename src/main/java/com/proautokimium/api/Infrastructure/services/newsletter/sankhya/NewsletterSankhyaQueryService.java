package com.proautokimium.api.Infrastructure.services.newsletter.sankhya;

import com.proautokimium.api.Infrastructure.services.sankhya.SankhyaQueryService;
import org.springframework.stereotype.Service;

@Service
public class NewsletterSankhyaQueryService {

    private final SankhyaQueryService  sankhyaQueryService;

    public NewsletterSankhyaQueryService(SankhyaQueryService queryService) {
        this.sankhyaQueryService = queryService;
    }
}
