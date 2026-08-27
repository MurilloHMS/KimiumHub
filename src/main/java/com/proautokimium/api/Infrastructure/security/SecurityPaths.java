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
            "/upload/profile/images/**",
            "/api/faq/public",
            "/api/profile/public/**",
            "/ws/**"
    };

    public static final String[] PUBLIC_POST ={
            "/api/auth/login",
            "/api/contact",
            "/api/certificate",
            "/api/certificate/no-validation",
            "/api/auth/forgot-password",
            "/api/auth/reset-password",
            "/api/candidatura",
            // As duas do fluxo de primeiro acesso, e só elas.
            //
            // Era "/api/auth/first-access/**", e esse padrão casa TAMBÉM o
            // caminho base — o que deixava `POST /api/auth/first-access`, que
            // dispara o e-mail de acesso, aberto para qualquer um. Quem valida
            // o token e quem escolhe a senha ainda não fez login e precisa
            // passar; quem MANDA o convite é o RH.
            "/api/auth/first-access/*/is-valid",
            "/api/auth/first-access/*/sign-in"
    };

    private SecurityPaths(){}
}
