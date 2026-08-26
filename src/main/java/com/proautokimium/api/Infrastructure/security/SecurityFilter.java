package com.proautokimium.api.Infrastructure.security;

import com.proautokimium.api.Infrastructure.repositories.UserRepository;
import com.proautokimium.api.Infrastructure.services.permission.PermissionService;
import com.proautokimium.api.domain.entities.auth.User;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import org.springframework.security.core.GrantedAuthority;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

@Component
public class SecurityFilter extends OncePerRequestFilter {

    @Autowired
    TokenService tokenService;

    @Autowired
    UserRepository userRepository;

    @Autowired
    PermissionService permissionService;

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain)
            throws ServletException, IOException {

        if ("OPTIONS".equalsIgnoreCase(request.getMethod())) {
            filterChain.doFilter(request, response);
            return;
        }

        var token = recoverToken(request);
        if (token != null) {
            var login = tokenService.validateToken(token);
            if (login != null) {
                UserDetails user = userRepository.findByLogin(login);
                if (user != null && ((User) user).isActive()) {
                    var authentication =
                            new UsernamePasswordAuthenticationToken(
                                    user,
                                    null,
                                    authoritiesOf((User) user)
                            );
                    SecurityContextHolder.getContext().setAuthentication(authentication);
                }
            }
        }

        filterChain.doFilter(request, response);
    }

    /**
     * As roles da pessoa mais as permissões de tela dela.
     *
     * A soma acontece **aqui**, e não em `User.getAuthorities()`, porque
     * entidade não injeta repositório — e as permissões vêm de uma consulta. O
     * filtro é onde o acesso ao banco já mora.
     *
     * As roles continuam: `hasRole('CLIENTE')` do `SecurityConfiguration`
     * depende delas, e o portal do cliente não participa do sistema de telas.
     */
    private List<GrantedAuthority> authoritiesOf(User user) {
        List<GrantedAuthority> authorities = new ArrayList<>(user.getAuthorities());
        authorities.addAll(permissionService.authoritiesOf(user.getId()));
        return authorities;
    }

    private String recoverToken(HttpServletRequest request){
        String authHeader = request.getHeader("Authorization");
        if(authHeader == null || !authHeader.startsWith("Bearer ")) return null;
        return authHeader.substring(7);
    }
}
