package com.proautokimium.api.Infrastructure.settings;

import org.springframework.boot.web.servlet.MultipartConfigFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.util.unit.DataSize;

import jakarta.servlet.MultipartConfigElement;

@Configuration
public class MultipartConfig {

    @Bean
    MultipartConfigElement multipartConfigElement() {
		MultipartConfigFactory factory = new MultipartConfigFactory();
		
		factory.setMaxFileSize(DataSize.ofMegabytes(50));
		factory.setMaxRequestSize(DataSize.ofGigabytes(1));
		return factory.createMultipartConfig();
	}
}
