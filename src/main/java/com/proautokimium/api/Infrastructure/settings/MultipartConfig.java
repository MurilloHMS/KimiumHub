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
		System.out.println("Iniciando configuração do multipart");
		factory.setMaxFileSize(DataSize.ofMegabytes(500));
		factory.setMaxRequestSize(DataSize.ofGigabytes(20));
		factory.setFileSizeThreshold(DataSize.ofMegabytes(10));
		return factory.createMultipartConfig();
	}
}
