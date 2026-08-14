package com.proautokimium.api.Infrastructure.services.authentication;

import com.proautokimium.api.Infrastructure.repositories.FirstAccessTokenRepository;
import com.proautokimium.api.Infrastructure.repositories.PasswordResetTokenRepository;
import com.proautokimium.api.domain.entities.Employee;
import com.proautokimium.api.domain.entities.Partner;
import com.proautokimium.api.domain.entities.auth.FirstAccessToken;
import com.proautokimium.api.domain.entities.auth.PasswordResetToken;
import com.proautokimium.api.domain.entities.auth.User;
import jakarta.transaction.Transactional;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.Optional;

@Service
public class TokenAuthService {

    /** Fonte única do TTL do token — também usado no e-mail que informa a validade ao usuário. */
    public static final int TOKEN_TTL_MINUTES = 30;

    /**
     * Convite de cliente. Trinta minutos serve para quem pediu o código e está
     * com a tela aberta; um convite chega sem ser pedido e pode esperar o
     * expediente seguinte.
     */
    public static final int INVITE_TTL_HOURS = 48;

    private final PasswordResetTokenRepository repositoryResetToken;
    private final FirstAccessTokenRepository repositoryFirstAccessToken;
    private final Clock clock;

    public TokenAuthService(PasswordResetTokenRepository repositoryResetToken, FirstAccessTokenRepository repositoryFirstAccessToken, Clock clock) {
        this.repositoryResetToken = repositoryResetToken;
        this.repositoryFirstAccessToken = repositoryFirstAccessToken;
        this.clock = clock;
    }

    public String createToken(User user) {
        String token = generateToken(6);

        PasswordResetToken resetToken = new PasswordResetToken();
        resetToken.setToken(token);
        resetToken.setUser(user);
        resetToken.setExpiration(expirationFromNow());

        repositoryResetToken.save(resetToken);
        return token;
    }

    public String createTokenByEmployee(Employee employee){
        // O funcionário digita o código à mão: curto, e vivo por pouco tempo.
        return createFirstAccess(employee, null, generateToken(6), expirationFromNow());
    }

    /**
     * O cliente recebe um link e nunca digita o código, então ele pode ser
     * longo — e precisa ser: são 48 horas de validade num endereço público,
     * onde seis caracteres seriam tentáveis um a um.
     */
    public String createInviteForPartner(Partner partner, String email){
        return createFirstAccess(partner, email, generateToken(32),
                LocalDateTime.now(clock).plusHours(INVITE_TTL_HOURS));
    }

    private String createFirstAccess(Partner partner, String email, String token, LocalDateTime expiration){
        FirstAccessToken accessToken = new FirstAccessToken();
        accessToken.setToken(token);
        accessToken.setPartner(partner);
        accessToken.setEmail(email);
        accessToken.setExpiration(expiration);

        repositoryFirstAccessToken.save(accessToken);
        return token;
    }

    public Optional<FirstAccessToken> getToken(String token){
        return repositoryFirstAccessToken.findByToken(token);
    }

    public Optional<FirstAccessToken> isValid(String token) {
        return repositoryFirstAccessToken.findByToken(token);
    }

    @Transactional
    public FirstAccessToken markTokenUsed(FirstAccessToken token){
        token.markUsed();
        return repositoryFirstAccessToken.save(token);
    }

    // Helpers

    private LocalDateTime expirationFromNow(){
        return LocalDateTime.now(clock).plusMinutes(TOKEN_TTL_MINUTES);
    }

    private String generateToken(int length){
        String characters = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789";
        SecureRandom random = new SecureRandom();
        StringBuilder token = new StringBuilder(length);

        for (int i = 0; i < length; i++) {
            token.append(characters.charAt(random.nextInt(characters.length())));
        }

        return token.toString();
    }

}
