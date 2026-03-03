package com.proautokimium.api.Infrastructure.services.authentication;

import com.proautokimium.api.Infrastructure.repositories.PasswordResetTokenRepository;
import com.proautokimium.api.domain.entities.PasswordResetToken;
import com.proautokimium.api.domain.entities.User;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.Base64;

@Service
public class PasswordResetService {

    private final PasswordResetTokenRepository repository;

    public PasswordResetService(PasswordResetTokenRepository repository) {
        this.repository = repository;
    }

    public String createToken(User user) {
        String characters = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789";
        SecureRandom random = new SecureRandom();
        StringBuilder token = new StringBuilder(5);

        for (int i = 0; i < 6; i++) {
            token.append(characters.charAt(random.nextInt(characters.length())));
        }

        PasswordResetToken resetToken = new PasswordResetToken();
        resetToken.setToken(token.toString());
        resetToken.setUser(user);
        resetToken.setExpiration(LocalDateTime.now().plusMinutes(30));

        repository.save(resetToken);
        return token.toString();
    }
}
