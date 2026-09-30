package com.proautokimium.api.Infrastructure.services.authentication.webauthn;

import com.proautokimium.api.Application.DTOs.user.LoginResponseDTO;
import com.proautokimium.api.Application.DTOs.webauthn.AuthenticateCredentialDTO;
import com.proautokimium.api.Application.DTOs.webauthn.AuthenticationOptionsDTO;
import com.proautokimium.api.Application.DTOs.webauthn.RegisterCredentialDTO;
import com.proautokimium.api.Application.DTOs.webauthn.RegistrationOptionsDTO;
import com.proautokimium.api.Application.DTOs.webauthn.WebAuthnCredentialDTO;
import com.proautokimium.api.Infrastructure.repositories.UserRepository;
import com.proautokimium.api.Infrastructure.repositories.WebAuthnCredentialRepository;
import com.proautokimium.api.Infrastructure.services.authentication.AuthenticationService;
import com.proautokimium.api.Infrastructure.services.notification.NotificationService;
import com.proautokimium.api.domain.entities.auth.User;
import com.proautokimium.api.domain.entities.auth.WebAuthnChallenge;
import com.proautokimium.api.domain.entities.auth.WebAuthnCredential;
import com.proautokimium.api.domain.enums.NotificationType;
import com.proautokimium.api.domain.enums.WebAuthnChallengePurpose;
import com.proautokimium.api.domain.exceptions.auth.UserNotFoundException;
import com.proautokimium.api.domain.exceptions.auth.WebAuthnAlreadyRegisteredException;
import com.proautokimium.api.domain.exceptions.auth.WebAuthnCredentialNotFoundException;
import com.proautokimium.api.domain.exceptions.auth.WebAuthnRegistrationRejectedException;
import com.proautokimium.api.domain.exceptions.auth.WebAuthnRejectedException;
import com.webauthn4j.WebAuthnManager;
import com.webauthn4j.converter.AttestedCredentialDataConverter;
import com.webauthn4j.credential.CredentialRecord;
import com.webauthn4j.credential.CredentialRecordImpl;
import com.webauthn4j.data.AuthenticationData;
import com.webauthn4j.data.AuthenticationParameters;
import com.webauthn4j.data.AuthenticationRequest;
import com.webauthn4j.data.AuthenticatorTransport;
import com.webauthn4j.data.PublicKeyCredentialParameters;
import com.webauthn4j.data.PublicKeyCredentialType;
import com.webauthn4j.data.RegistrationData;
import com.webauthn4j.data.RegistrationParameters;
import com.webauthn4j.data.RegistrationRequest;
import com.webauthn4j.data.attestation.authenticator.AttestedCredentialData;
import com.webauthn4j.data.attestation.authenticator.AuthenticatorData;
import com.webauthn4j.data.attestation.statement.COSEAlgorithmIdentifier;
import com.webauthn4j.data.attestation.statement.NoneAttestationStatement;
import com.webauthn4j.data.client.challenge.DefaultChallenge;
import com.webauthn4j.data.extension.authenticator.AuthenticationExtensionsAuthenticatorOutputs;
import com.webauthn4j.data.extension.authenticator.RegistrationExtensionAuthenticatorOutput;
import com.webauthn4j.data.extension.client.AuthenticationExtensionsClientOutputs;
import com.webauthn4j.server.ServerProperty;
import com.webauthn4j.util.Base64UrlUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Entrar com a digital (WebAuthn / passkeys).
 *
 * <h2>O que o servidor guarda, e o que não</h2>
 *
 * O aparelho cria um par de chaves e guarda a privada. Aqui chega só a
 * pública, e é com ela que se confere a assinatura de cada login. Nenhum dado
 * biométrico passa pela API: a digital fica no aparelho, que só responde
 * "confirmei" assinando o desafio.
 *
 * <h2>As duas cerimônias</h2>
 *
 * <b>Cadastro</b> — a pessoa já entrou com a senha. O servidor emite um
 * desafio dela; o aparelho pede a digital, cria a chave e devolve a pública
 * assinada junto com o desafio; a webauthn4j confere e a credencial é gravada.
 *
 * <p><b>Login</b> — o servidor emite um desafio SEM saber quem é. O aparelho,
 * que guardou o id da credencial, pede a digital e assina. Só na volta o id
 * diz de quem é, e a assinatura prova que é mesmo. Assim nenhum endpoint
 * responde "esse login tem digital".
 *
 * <p>Toda recusa de login é a mesma {@link WebAuthnRejectedException}; o motivo
 * real vai para o log.
 */
@Service
public class WebAuthnService {

    private static final Logger log = LoggerFactory.getLogger(WebAuthnService.class);

    /**
     * Os algoritmos aceitos. ES256 é o que os celulares e o Windows Hello usam;
     * RS256 cobre leitores mais antigos no Windows. A mesma lista vai nas
     * opções do cadastro — o site a repete na chamada ao navegador.
     */
    static final List<PublicKeyCredentialParameters> ALGORITHMS = List.of(
            new PublicKeyCredentialParameters(PublicKeyCredentialType.PUBLIC_KEY, COSEAlgorithmIdentifier.ES256),
            new PublicKeyCredentialParameters(PublicKeyCredentialType.PUBLIC_KEY, COSEAlgorithmIdentifier.RS256));

    private final WebAuthnCredentialRepository credentials;
    private final WebAuthnChallengeStore challenges;
    private final UserRepository users;
    private final WebAuthnManager webAuthn;
    private final AttestedCredentialDataConverter credentialDataConverter;
    private final WebAuthnSettings settings;
    private final AuthenticationService authentication;
    private final NotificationService notifications;
    private final Clock clock;

    public WebAuthnService(WebAuthnCredentialRepository credentials, WebAuthnChallengeStore challenges,
                           UserRepository users, WebAuthnManager webAuthn,
                           AttestedCredentialDataConverter credentialDataConverter, WebAuthnSettings settings,
                           AuthenticationService authentication, NotificationService notifications, Clock clock) {
        this.credentials = credentials;
        this.challenges = challenges;
        this.users = users;
        this.webAuthn = webAuthn;
        this.credentialDataConverter = credentialDataConverter;
        this.settings = settings;
        this.authentication = authentication;
        this.notifications = notifications;
        this.clock = clock;
    }

    // ── Cadastro ─────────────────────────────────────────────────────────────

    @Transactional
    public RegistrationOptionsDTO registrationOptions(String login) {
        User user = user(login);
        WebAuthnChallenge challenge = challenges.issueForRegistration(user.getId());
        List<String> alreadyRegistered = credentials.findByUserIdOrderByCreatedAtDesc(user.getId()).stream()
                .map(WebAuthnCredential::getCredentialId)
                .toList();
        String displayName = user.getEmployee() != null ? user.getEmployee().getName() : user.getLogin();
        return new RegistrationOptionsDTO(
                challenge.getId(),
                Base64UrlUtil.encodeToString(challenge.getChallenge()),
                settings.rpId(),
                settings.rpName(),
                Base64UrlUtil.encodeToString(userHandle(user.getId())),
                user.getLogin(),
                displayName,
                alreadyRegistered,
                WebAuthnChallenge.TTL.toMillis());
    }

    /**
     * Confere e grava a digital nova. O desafio é gasto antes da verificação,
     * em transação própria (WebAuthnChallengeStore): falhou, não se tenta de
     * novo com ele.
     */
    @Transactional
    public WebAuthnCredentialDTO register(String login, RegisterCredentialDTO dto, String userAgent) {
        User user = user(login);
        WebAuthnChallenge challenge = challenges.consume(dto.challengeId(), WebAuthnChallengePurpose.REGISTRATION, user.getId());

        RegistrationData data;
        try {
            RegistrationRequest request = new RegistrationRequest(
                    Base64UrlUtil.decode(dto.attestationObject()),
                    Base64UrlUtil.decode(dto.clientDataJSON()),
                    null,
                    dto.transports() == null ? null : Set.copyOf(dto.transports()));
            // true, true: a digital (ou o PIN) é exigida, e a presença também.
            data = webAuthn.verify(request, new RegistrationParameters(server(challenge), ALGORITHMS, true, true));
        } catch (RuntimeException e) {
            log.warn("[WebAuthn] cadastro recusado para {}: {}", login, e.toString());
            throw new WebAuthnRegistrationRejectedException();
        }

        AuthenticatorData<RegistrationExtensionAuthenticatorOutput> authenticatorData =
                data.getAttestationObject().getAuthenticatorData();
        AttestedCredentialData credentialData = authenticatorData.getAttestedCredentialData();
        String credentialId = Base64UrlUtil.encodeToString(credentialData.getCredentialId());
        if (credentials.existsByCredentialId(credentialId)) {
            throw new WebAuthnAlreadyRegisteredException();
        }

        WebAuthnCredential saved = credentials.save(WebAuthnCredential.register(
                user.getId(),
                credentialId,
                credentialDataConverter.convert(credentialData),
                authenticatorData.getSignCount(),
                authenticatorData.isFlagUV(),
                authenticatorData.isFlagBE(),
                authenticatorData.isFlagBS(),
                dto.transports() == null ? null : String.join(",", dto.transports()),
                DeviceLabel.from(userAgent),
                now()));
        return WebAuthnCredentialDTO.from(saved);
    }

    // ── Login ────────────────────────────────────────────────────────────────

    public AuthenticationOptionsDTO authenticationOptions() {
        WebAuthnChallenge challenge = challenges.issueForAuthentication();
        return new AuthenticationOptionsDTO(
                challenge.getId(),
                Base64UrlUtil.encodeToString(challenge.getChallenge()),
                settings.rpId(),
                WebAuthnChallenge.TTL.toMillis());
    }

    /**
     * Confere a assinatura e abre a sessão pelo mesmo caminho do login com
     * senha ({@link AuthenticationService#issueSession}): conta bloqueada não
     * entra por nenhuma das duas portas.
     */
    @Transactional
    public LoginResponseDTO login(AuthenticateCredentialDTO dto) {
        WebAuthnChallenge challenge = challenges.consume(dto.challengeId(), WebAuthnChallengePurpose.AUTHENTICATION, null);

        WebAuthnCredential credential = credentials.findByCredentialId(dto.credentialId()).orElseThrow(() -> {
            log.warn("[WebAuthn] login com credencial desconhecida");
            return new WebAuthnRejectedException();
        });

        AuthenticationData data;
        try {
            byte[] credentialId = Base64UrlUtil.decode(dto.credentialId());
            byte[] userHandle = dto.userHandle() == null || dto.userHandle().isBlank()
                    ? null : Base64UrlUtil.decode(dto.userHandle());
            // O aparelho diz de quem é a credencial; tem de bater com o dono gravado.
            if (userHandle != null && !Arrays.equals(userHandle, userHandle(credential.getUserId()))) {
                throw new IllegalArgumentException("user handle de outra pessoa");
            }
            AuthenticationRequest request = new AuthenticationRequest(
                    credentialId,
                    userHandle,
                    Base64UrlUtil.decode(dto.authenticatorData()),
                    Base64UrlUtil.decode(dto.clientDataJSON()),
                    null,
                    Base64UrlUtil.decode(dto.signature()));
            data = webAuthn.verify(request, new AuthenticationParameters(
                    server(challenge), record(credential), List.of(credentialId), true, true));
        } catch (RuntimeException e) {
            log.warn("[WebAuthn] login recusado para a credencial {}: {}", credential.getId(), e.toString());
            throw new WebAuthnRejectedException();
        }

        credential.recordUse(data.getAuthenticatorData().getSignCount(), data.getAuthenticatorData().isFlagBS(), now());
        credentials.save(credential);

        User user = users.findById(credential.getUserId()).orElseThrow(WebAuthnRejectedException::new);
        return authentication.issueSession(user);
    }

    // ── Os aparelhos de uma pessoa ───────────────────────────────────────────

    @Transactional(readOnly = true)
    public List<WebAuthnCredentialDTO> listMine(String login) {
        return list(user(login).getId());
    }

    /** Só apaga credencial SUA: a de outra pessoa dá o mesmo 404 da inexistente. */
    @Transactional
    public void removeMine(String login, UUID id) {
        User user = user(login);
        WebAuthnCredential credential = credentials.findById(id)
                .filter(c -> c.belongsTo(user.getId()))
                .orElseThrow(WebAuthnCredentialNotFoundException::new);
        credentials.delete(credential);
    }

    // ── Pelo RH / ADMIN, no cadastro do funcionário ──────────────────────────

    /** Funcionário sem usuário não tem digital: lista vazia, e não erro. */
    @Transactional(readOnly = true)
    public List<WebAuthnCredentialDTO> listForEmployee(UUID employeeId) {
        return users.findByEmployee_Id(employeeId).map(u -> list(u.getId())).orElse(List.of());
    }

    /**
     * Remove um aparelho do funcionário, ou todos ({@code credentialId} nulo) —
     * o caso de quem sai da empresa ou perde o celular. A pessoa é avisada no
     * sino, para não achar que a digital quebrou.
     */
    @Transactional
    public void removeForEmployee(UUID employeeId, UUID credentialId) {
        User user = users.findByEmployee_Id(employeeId).orElseThrow(WebAuthnCredentialNotFoundException::new);
        List<WebAuthnCredential> targets = credentials.findByUserIdOrderByCreatedAtDesc(user.getId()).stream()
                .filter(c -> credentialId == null || c.getId().equals(credentialId))
                .toList();
        if (targets.isEmpty()) {
            if (credentialId != null) throw new WebAuthnCredentialNotFoundException();
            return;
        }
        credentials.deleteAll(targets);

        String what = targets.size() == 1
                ? "do aparelho " + targets.get(0).getDeviceLabel()
                : "dos seus " + targets.size() + " aparelhos";
        notifications.notify(user.getLogin(), NotificationType.GERAL, "Entrada com digital removida",
                "A entrada com digital " + what + " foi removida pela empresa. Para usar de novo, entre com a senha e ative outra vez.",
                "/perfil");
    }

    // ── Apoio ────────────────────────────────────────────────────────────────

    private List<WebAuthnCredentialDTO> list(String userId) {
        return credentials.findByUserIdOrderByCreatedAtDesc(userId).stream().map(WebAuthnCredentialDTO::from).toList();
    }

    private User user(String login) {
        return users.findByLoginWithEmployee(login).orElseThrow(UserNotFoundException::new);
    }

    /**
     * O "user handle" que o aparelho guarda junto com a chave: o id interno,
     * nunca o login ou o e-mail — ele fica no aparelho e pode ser sincronizado.
     */
    private static byte[] userHandle(String userId) {
        return userId.getBytes(StandardCharsets.UTF_8);
    }

    /** Domínio, origens aceitas e o desafio que o aparelho precisa ter assinado. */
    private ServerProperty server(WebAuthnChallenge challenge) {
        return new ServerProperty(settings.origins(), settings.rpId(), new DefaultChallenge(challenge.getChallenge()));
    }

    /** A credencial gravada, no formato que a webauthn4j confere. */
    private CredentialRecord record(WebAuthnCredential credential) {
        Set<AuthenticatorTransport> transports = credential.getTransports() == null ? null
                : Arrays.stream(credential.getTransports().split(","))
                        .filter(t -> !t.isBlank())
                        .map(AuthenticatorTransport::create)
                        .collect(Collectors.toSet());
        return new CredentialRecordImpl(
                new NoneAttestationStatement(),
                credential.isUvInitialized(),
                credential.isBackupEligible(),
                credential.isBackupState(),
                credential.getSignCount(),
                credentialDataConverter.convert(credential.getAttestedCredentialData()),
                new AuthenticationExtensionsAuthenticatorOutputs<>(),
                null,
                new AuthenticationExtensionsClientOutputs<>(),
                transports);
    }

    private LocalDateTime now() {
        return LocalDateTime.now(clock);
    }
}
