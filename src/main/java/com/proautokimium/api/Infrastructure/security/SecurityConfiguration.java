package com.proautokimium.api.Infrastructure.security;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.access.expression.WebExpressionAuthorizationManager;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.List;

@Configuration
@EnableWebSecurity
public class SecurityConfiguration {
    @Autowired
    SecurityFilter securityFilter;

    @Bean
    SecurityFilterChain securityFilterChain(HttpSecurity httpSecurity) throws Exception{
        return httpSecurity
                .csrf(AbstractHttpConfigurer::disable)
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(authorize -> authorize
                        .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                        .requestMatchers(HttpMethod.POST, SecurityPaths.PUBLIC_POST).permitAll()
                        .requestMatchers(HttpMethod.GET, SecurityPaths.PUBLIC_GET).permitAll()
                        .requestMatchers(HttpMethod.PUT, SecurityPaths.PUBLIC_PUT).permitAll()
                        .requestMatchers(HttpMethod.DELETE, SecurityPaths.PUBLIC_DELETE).permitAll()
                        .requestMatchers(SecurityPaths.SWAGGER).permitAll()
                        .requestMatchers("api/client/**").hasRole("CLIENTE")
                        .anyRequest().access(new WebExpressionAuthorizationManager("isAuthenticated() and !hasRole('CLIENTE')"))
                )
                .addFilterBefore(securityFilter, UsernamePasswordAuthenticationFilter.class)
                .build();
    }

    @Bean
    AuthenticationManager authenticationManager(AuthenticationConfiguration authenticationConfiguration) throws Exception{
        return authenticationConfiguration.getAuthenticationManager();
    }

    @Bean
    PasswordEncoder passwordEncoder(){ return new BCryptPasswordEncoder(); }

    @Bean
    CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOriginPatterns(List.of(
                        "https://*.proautokimium.com",
                        "https://*.proautokimium.com.br",
                        "https://proautokimium.com",
                        "https://proautokimium.com.br")
                );
        
        // PATCH faltava até 2026-10-08, e três telas usam: renomear modelo de
        // permissão, editar remetente de e-mail e trocar o e-mail de uma conta.
        // O site e a API ficam em domínios diferentes, então o navegador pergunta
        // antes (preflight) e recusava os três em produção. No ambiente local o
        // proxy deixa tudo no mesmo domínio, e por isso ninguém viu.
        configuration.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        configuration.setAllowedHeaders(List.of("Authorization", "Content-Type", "Accept", "Origin"));
        // Content-Disposition traz o nome do arquivo dos downloads (relatórios,
        // planilhas). Sem expor, o site não lê e salva com um nome genérico.
        configuration.setExposedHeaders(List.of("Authorization", "Content-Disposition"));
        configuration.setAllowCredentials(true);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }
}
