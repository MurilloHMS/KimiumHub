package com.proautokimium.api.Infrastructure.interfaces.email.newsletter;

import org.springframework.web.multipart.MultipartFile;

public interface INewsletterOrchestrator {
	void executeMonthlyNewsletter();
    void includeMonthlyNewsletterByExcel(MultipartFile file);
}
