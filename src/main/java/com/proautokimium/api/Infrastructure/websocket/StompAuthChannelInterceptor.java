package com.proautokimium.api.Infrastructure.websocket;

import com.proautokimium.api.Infrastructure.repositories.UserRepository;
import com.proautokimium.api.Infrastructure.security.TokenService;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;

/**
 * Autentica a conexão STOMP no frame CONNECT lendo o "Authorization: Bearer <token>".
 * Definir o Principal aqui faz o roteamento /user/{login}/queue/... apontar para o usuário certo.
 */
@Component
public class StompAuthChannelInterceptor implements ChannelInterceptor {

    private final TokenService tokenService;
    private final UserRepository userRepository;

    public StompAuthChannelInterceptor(TokenService tokenService, UserRepository userRepository) {
        this.tokenService = tokenService;
        this.userRepository = userRepository;
    }

    @Override
    public Message<?> preSend(Message<?> message, MessageChannel channel) {
        StompHeaderAccessor accessor = MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);

        if (accessor != null && StompCommand.CONNECT.equals(accessor.getCommand())) {
            String authHeader = accessor.getFirstNativeHeader("Authorization");
            if (authHeader != null && authHeader.startsWith("Bearer ")) {
                String token = authHeader.substring(7);
                String login = tokenService.validateToken(token);
                if (login != null && !login.isBlank()) {
                    UserDetails user = userRepository.findByLogin(login);
                    if (user != null) {
                        var authentication = new UsernamePasswordAuthenticationToken(
                                user, null, user.getAuthorities());
                        accessor.setUser(authentication);
                    }
                }
            }
        }

        return message;
    }
}
