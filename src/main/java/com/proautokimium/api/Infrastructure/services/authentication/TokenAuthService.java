package com.proautokimium.api.Infrastructure.services.authentication;

import com.proautokimium.api.Infrastructure.repositories.FirstAccessTokenRepository;
import com.proautokimium.api.Infrastructure.repositories.PasswordResetTokenRepository;
import com.proautokimium.api.domain.entities.Employee;
import com.proautokimium.api.domain.entities.auth.FirstAcessToken;
import com.proautokimium.api.domain.entities.auth.PasswordResetToken;
import com.proautokimium.api.domain.entities.auth.User;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.Optional;

@Service
public class TokenAuthService {

    private static final int TOKEN_TTL_MINUTES = 30;

    private final PasswordResetTokenRepository repositoryResetToken;
    private final FirstAccessTokenRepository repositoryFirstAccessToken;
    private final Clock clock;

    public TokenAuthService(PasswordResetTokenRepository repositoryResetToken, FirstAccessTokenRepository repositoryFirstAccessToken, Clock clock) {
        this.repositoryResetToken = repositoryResetToken;
        this.repositoryFirstAccessToken = repositoryFirstAccessToken;
        this.clock = clock;
    }

    public String createToken(User user) {
        String token = generateToken();

        PasswordResetToken resetToken = new PasswordResetToken();
        resetToken.setToken(token);
        resetToken.setUser(user);
        resetToken.setExpiration(expirationFromNow());

        repositoryResetToken.save(resetToken);
        return token;
    }

    public String createTokenByEmployee(Employee employee){
        String token = generateToken();

        FirstAcessToken accessToken = new FirstAcessToken();
        accessToken.setToken(token);
        accessToken.setEmployee(employee);
        accessToken.setExpiration(expirationFromNow());

        repositoryFirstAccessToken.save(accessToken);
        return token;
    }

    public Optional<FirstAcessToken> getToken(String token){
        return repositoryFirstAccessToken.findByToken(token);
    }

    public Optional<FirstAcessToken> isValid(String token) {
        return repositoryFirstAccessToken.findByToken(token);
    }

    // Helpers

    private LocalDateTime expirationFromNow(){
        return LocalDateTime.now(clock).plusMinutes(TOKEN_TTL_MINUTES);
    }
    private String generateToken(){
        String characters = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789";
        SecureRandom random = new SecureRandom();
        StringBuilder token = new StringBuilder(5);

        for (int i = 0; i < 6; i++) {
            token.append(characters.charAt(random.nextInt(characters.length())));
        }

        return token.toString();
    }

}
