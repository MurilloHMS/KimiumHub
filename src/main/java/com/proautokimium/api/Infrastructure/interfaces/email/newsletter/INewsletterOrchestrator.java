package com.proautokimium.api.Infrastructure.interfaces.email.newsletter;

import java.util.List;

import org.springframework.web.multipart.MultipartFile;

public interface INewsletterOrchestrator {
	void executeMonthlyNewsletter();
	void includeMonthlyNewsletter(List<MultipartFile> files);
}
