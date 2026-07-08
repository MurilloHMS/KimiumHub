package com.proautokimium.api.Infrastructure.security;

public final class SecurityPaths {

    public static final String[] SWAGGER = {
            "/v3/api-docs/**",
            "/swagger-ui/**",
            "/swagger-ui.html"
    };

    public static final String[] PUBLIC_GET ={
            "/api/vaga/publicadas",
            "/api/curriculos",
            "/api/public-secrets/**",
            "/api/product/website/active",
            "/upload/images/**",
            "/upload/equipment/images/**",
            "/api/faq/public",
            "/api/profile/public/**",
            "/ws/**"
    };

    public static final String[] PUBLIC_POST ={
            "/api/auth/login",
            "/api/auth/login/android",
            "/api/contact",
            "/api/certificate",
            "/api/certificate/no-validation",
            "/api/auth/forgot-password",
            "/api/auth/reset-password",
            "/api/candidatura",
            "/api/auth/first-access/**"
    };

    private SecurityPaths(){}
}
