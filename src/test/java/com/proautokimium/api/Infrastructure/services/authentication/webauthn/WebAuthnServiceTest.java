package com.proautokimium.api.Infrastructure.services.authentication.webauthn;

import com.proautokimium.api.Application.DTOs.user.LoginResponseDTO;
import com.proautokimium.api.Application.DTOs.webauthn.AuthenticateCredentialDTO;
import com.proautokimium.api.Application.DTOs.webauthn.AuthenticationOptionsDTO;
import com.proautokimium.api.Application.DTOs.webauthn.RegisterCredentialDTO;
import com.proautokimium.api.Application.DTOs.webauthn.RegistrationOptionsDTO;
import com.proautokimium.api.Infrastructure.repositories.UserRepository;
import com.proautokimium.api.Infrastructure.repositories.WebAuthnChallengeRepository;
import com.proautokimium.api.Infrastructure.repositories.WebAuthnCredentialRepository;
import com.proautokimium.api.Infrastructure.services.authentication.AuthenticationService;
import com.proautokimium.api.Infrastructure.services.notification.NotificationService;
import com.proautokimium.api.domain.entities.auth.User;
import com.proautokimium.api.domain.entities.auth.WebAuthnChallenge;
import com.proautokimium.api.domain.entities.auth.WebAuthnCredential;
import com.proautokimium.api.domain.enums.NotificationType;
import com.proautokimium.api.domain.enums.UserRole;
import com.proautokimium.api.domain.exceptions.auth.WebAuthnAlreadyRegisteredException;
import com.proautokimium.api.domain.exceptions.auth.WebAuthnChallengeExpiredException;
import com.proautokimium.api.domain.exceptions.auth.WebAuthnCredentialNotFoundException;
import com.proautokimium.api.domain.exceptions.auth.WebAuthnRegistrationRejectedException;
import com.proautokimium.api.domain.exceptions.auth.WebAuthnRejectedException;
import com.webauthn4j.WebAuthnManager;
import com.webauthn4j.converter.AttestedCredentialDataConverter;
import com.webauthn4j.converter.util.ObjectConverter;
import com.webauthn4j.util.Base64UrlUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/**
 * As duas cerimônias com assinatura DE VERDADE ({@link FakeAuthenticator}) e a
 * webauthn4j real: o que passa aqui é o que o celular vai mandar. Só o banco é
 * de mentira (mapas em memória atrás dos repositórios).
 */
class WebAuthnServiceTest {

    static final String ORIGIN = "http://localhost:4200";
    static final String RP_ID = "localhost";
    static final String ANDROID = "Mozilla/5.0 (Linux; Android 14; SM-A546E) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/129.0 Mobile Safari/537.36";
    static final UUID EMPLOYEE_ID = UUID.fromString("0b0e5a3c-5f1e-4a8d-9d2b-3c1f6a7e8d90");

    final MovableClock clock = new MovableClock(Instant.parse("2026-09-30T13:00:00Z"));

    final WebAuthnCredentialRepository credentials = mock(WebAuthnCredentialRepository.class);
    final WebAuthnChallengeRepository challengeRepository = mock(WebAuthnChallengeRepository.class);
    final UserRepository users = mock(UserRepository.class);
    final AuthenticationService authentication = mock(AuthenticationService.class);
    final NotificationService notifications = mock(NotificationService.class);

    final Map<UUID, WebAuthnChallenge> storedChallenges = new HashMap<>();
    final List<WebAuthnCredential> storedCredentials = new ArrayList<>();

    User diego;
    User ana;
    WebAuthnService service;

    @BeforeEach
    void setUp() {
        diego = user("u-diego", "diego");
        ana = user("u-ana", "ana");
        fakeDatabase();

        ObjectConverter converter = new ObjectConverter();
        service = new WebAuthnService(credentials, new WebAuthnChallengeStore(challengeRepository, clock), users,
                WebAuthnManager.createNonStrictWebAuthnManager(converter), new AttestedCredentialDataConverter(converter),
                new WebAuthnSettings(RP_ID, "KimiumHub", ORIGIN), authentication, notifications, clock);
        when(authentication.issueSession(any())).thenAnswer(inv ->
                new LoginResponseDTO("access-" + ((User) inv.getArgument(0)).getLogin(), "refresh"));
    }

    // ── Cadastro ─────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("cadastro da digital")
    class Registration {

        @Test
        @DisplayName("a assinatura real passa, e grava a chave pública com o nome do aparelho")
        void registersWithRealSignature() {
            FakeAuthenticator phone = registered(diego);

            assertThat(storedCredentials).hasSize(1);
            WebAuthnCredential saved = storedCredentials.get(0);
            assertThat(saved.getUserId()).isEqualTo("u-diego");
            assertThat(saved.getCredentialId()).isEqualTo(phone.credentialIdBase64());
            assertThat(saved.getDeviceLabel()).isEqualTo("Android · Chrome");
            assertThat(saved.isUvInitialized()).as("a digital foi exigida").isTrue();
            assertThat(saved.getTransports()).isEqualTo("internal");
            assertThat(saved.getCreatedAt()).isEqualTo(LocalDateTime.of(2026, 9, 30, 10, 0));
        }

        @Test
        @DisplayName("as opções levam o id interno como user handle, e as digitais que a pessoa já tem")
        void optionsCarryHandleAndExistingCredentials() {
            FakeAuthenticator phone = registered(diego);

            RegistrationOptionsDTO options = service.registrationOptions("diego");

            assertThat(options.userId()).isEqualTo(Base64UrlUtil.encodeToString("u-diego".getBytes(StandardCharsets.UTF_8)));
            assertThat(options.userName()).isEqualTo("diego");
            assertThat(options.rpId()).isEqualTo(RP_ID);
            assertThat(options.excludeCredentialIds()).containsExactly(phone.credentialIdBase64());
            assertThat(options.timeoutMs()).isEqualTo(Duration.ofMinutes(5).toMillis());
        }

        @Test
        @DisplayName("vindo de outro site: recusa com 400 (não derruba a sessão), e o desafio fica gasto")
        void wrongOriginIsRejectedAndChallengeBurned() {
            RegistrationOptionsDTO options = service.registrationOptions("diego");
            RegisterCredentialDTO fromElsewhere = new FakeAuthenticator().register(options, "https://proautokimium-login.com");

            assertThatThrownBy(() -> service.register("diego", fromElsewhere, ANDROID))
                    .isInstanceOf(WebAuthnRegistrationRejectedException.class);
            assertThat(storedCredentials).isEmpty();
            assertThat(storedChallenges.get(options.challengeId()).getUsedAt())
                    .as("falhou, mas não se tenta de novo com o mesmo desafio").isNotNull();
        }

        @Test
        @DisplayName("sem a digital verificada (só um toque), recusa")
        void withoutUserVerificationRejected() {
            RegistrationOptionsDTO options = service.registrationOptions("diego");
            FakeAuthenticator phone = new FakeAuthenticator();
            phone.userVerified = false;

            assertThatThrownBy(() -> service.register("diego", phone.register(options, ORIGIN), ANDROID))
                    .isInstanceOf(WebAuthnRegistrationRejectedException.class);
        }

        @Test
        @DisplayName("o desafio de outra pessoa não serve")
        void challengeOfAnotherUserRefused() {
            RegistrationOptionsDTO anasOptions = service.registrationOptions("ana");

            assertThatThrownBy(() -> service.register("diego", new FakeAuthenticator().register(anasOptions, ORIGIN), ANDROID))
                    .isInstanceOf(WebAuthnChallengeExpiredException.class);
        }

        @Test
        @DisplayName("no instante exato dos 5 minutos o desafio já venceu")
        void challengeExpiresAtFiveMinutes() {
            RegistrationOptionsDTO options = service.registrationOptions("diego");
            clock.advance(Duration.ofMinutes(5));

            assertThatThrownBy(() -> service.register("diego", new FakeAuthenticator().register(options, ORIGIN), ANDROID))
                    .isInstanceOf(WebAuthnChallengeExpiredException.class);
        }

        @Test
        @DisplayName("o mesmo aparelho duas vezes: 409")
        void sameCredentialTwiceConflicts() {
            FakeAuthenticator phone = registered(diego);
            RegistrationOptionsDTO again = service.registrationOptions("diego");

            assertThatThrownBy(() -> service.register("diego", phone.register(again, ORIGIN), ANDROID))
                    .isInstanceOf(WebAuthnAlreadyRegisteredException.class);
            assertThat(storedCredentials).hasSize(1);
        }
    }

    // ── Login ────────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("login com a digital")
    class Login {

        @Test
        @DisplayName("a assinatura real abre a sessão pelo mesmo caminho da senha, e guarda o contador")
        void loginOpensSession() {
            FakeAuthenticator phone = registered(diego);
            clock.advance(Duration.ofHours(2));
            phone.signCount = 1;

            LoginResponseDTO session = service.login(phone.login(service.authenticationOptions(), ORIGIN, handle(diego)));

            assertThat(session.token()).isEqualTo("access-diego");
            verify(authentication).issueSession(diego);
            WebAuthnCredential used = storedCredentials.get(0);
            assertThat(used.getSignCount()).isEqualTo(1);
            assertThat(used.getLastUsedAt()).isEqualTo(LocalDateTime.of(2026, 9, 30, 12, 0));
        }

        @Test
        @DisplayName("sem user handle (alguns aparelhos não mandam) também entra")
        void loginWithoutUserHandle() {
            FakeAuthenticator phone = registered(diego);

            assertThat(service.login(phone.login(service.authenticationOptions(), ORIGIN, null)).token())
                    .isEqualTo("access-diego");
        }

        @Test
        @DisplayName("assinatura adulterada: recusa, e ninguém entra")
        void tamperedSignatureRejected() {
            FakeAuthenticator phone = registered(diego);
            AuthenticateCredentialDTO real = phone.login(service.authenticationOptions(), ORIGIN, handle(diego));
            byte[] signature = Base64UrlUtil.decode(real.signature());
            signature[signature.length - 1] ^= 0x01;
            AuthenticateCredentialDTO tampered = new AuthenticateCredentialDTO(real.challengeId(), real.credentialId(),
                    real.clientDataJSON(), real.authenticatorData(), Base64UrlUtil.encodeToString(signature), real.userHandle());

            assertThatThrownBy(() -> service.login(tampered)).isInstanceOf(WebAuthnRejectedException.class);
            verify(authentication, never()).issueSession(any());
        }

        @Test
        @DisplayName("assinado por um aparelho que não é o cadastrado: recusa")
        void otherKeyRejected() {
            FakeAuthenticator phone = registered(diego);
            AuthenticationOptionsDTO options = service.authenticationOptions();
            AuthenticateCredentialDTO impostor = new FakeAuthenticator().login(options, ORIGIN, handle(diego));
            // O id da credencial é público; a chave privada não.
            AuthenticateCredentialDTO withRealId = new AuthenticateCredentialDTO(impostor.challengeId(), phone.credentialIdBase64(),
                    impostor.clientDataJSON(), impostor.authenticatorData(), impostor.signature(), impostor.userHandle());

            assertThatThrownBy(() -> service.login(withRealId)).isInstanceOf(WebAuthnRejectedException.class);
        }

        @Test
        @DisplayName("contador que anda para trás é credencial copiada: recusa")
        void counterGoingBackwardsRejected() {
            FakeAuthenticator phone = registered(diego);
            phone.signCount = 5;
            service.login(phone.login(service.authenticationOptions(), ORIGIN, handle(diego)));

            phone.signCount = 3;
            AuthenticateCredentialDTO copy = phone.login(service.authenticationOptions(), ORIGIN, handle(diego));

            assertThatThrownBy(() -> service.login(copy)).isInstanceOf(WebAuthnRejectedException.class);
        }

        @Test
        @DisplayName("o mesmo desafio não entra duas vezes")
        void challengeCannotBeReused() {
            FakeAuthenticator phone = registered(diego);
            AuthenticationOptionsDTO options = service.authenticationOptions();
            phone.signCount = 1;
            service.login(phone.login(options, ORIGIN, handle(diego)));

            phone.signCount = 2;
            AuthenticateCredentialDTO replay = phone.login(options, ORIGIN, handle(diego));

            assertThatThrownBy(() -> service.login(replay)).isInstanceOf(WebAuthnChallengeExpiredException.class);
        }

        @Test
        @DisplayName("desafio de cadastro não serve para entrar")
        void registrationChallengeCannotLogIn() {
            FakeAuthenticator phone = registered(diego);
            RegistrationOptionsDTO registration = service.registrationOptions("diego");

            AuthenticateCredentialDTO dto = phone.login(registration.challengeId(), registration.challenge(), RP_ID, ORIGIN, handle(diego));

            assertThatThrownBy(() -> service.login(dto)).isInstanceOf(WebAuthnChallengeExpiredException.class);
        }

        @Test
        @DisplayName("credencial desconhecida: a mesma recusa, sem dizer o motivo")
        void unknownCredentialRejected() {
            AuthenticateCredentialDTO dto = new FakeAuthenticator().login(service.authenticationOptions(), ORIGIN, null);

            assertThatThrownBy(() -> service.login(dto))
                    .isInstanceOf(WebAuthnRejectedException.class)
                    .hasMessage("Não foi possível confirmar pela digital. Tente de novo, ou entre com a senha.");
        }

        @Test
        @DisplayName("o aparelho dizendo que a credencial é de outra pessoa: recusa")
        void userHandleOfAnotherPersonRejected() {
            FakeAuthenticator phone = registered(diego);

            AuthenticateCredentialDTO dto = phone.login(service.authenticationOptions(), ORIGIN, handle(ana));

            assertThatThrownBy(() -> service.login(dto)).isInstanceOf(WebAuthnRejectedException.class);
            verify(authentication, never()).issueSession(any());
        }

        @Test
        @DisplayName("assinado para outro domínio: recusa")
        void wrongRpIdRejected() {
            FakeAuthenticator phone = registered(diego);
            AuthenticationOptionsDTO options = service.authenticationOptions();

            AuthenticateCredentialDTO dto = phone.login(options.challengeId(), options.challenge(), "proautokimium-login.com", ORIGIN, null);

            assertThatThrownBy(() -> service.login(dto)).isInstanceOf(WebAuthnRejectedException.class);
        }
    }

    // ── Os aparelhos ─────────────────────────────────────────────────────────

    @Nested
    @DisplayName("remover aparelhos")
    class Removal {

        @Test
        @DisplayName("a pessoa remove o próprio aparelho")
        void removeMine() {
            registered(diego);

            service.removeMine("diego", storedCredentials.get(0).getId());

            assertThat(storedCredentials).isEmpty();
        }

        @Test
        @DisplayName("aparelho de outra pessoa dá o mesmo 404 do inexistente, e fica onde está")
        void removeMineOfAnotherPersonIsNotFound() {
            registered(ana);
            UUID anasDevice = storedCredentials.get(0).getId();

            assertThatThrownBy(() -> service.removeMine("diego", anasDevice))
                    .isInstanceOf(WebAuthnCredentialNotFoundException.class);
            assertThat(storedCredentials).hasSize(1);
        }

        @Test
        @DisplayName("o RH remove todos, e a pessoa é avisada uma vez no sino")
        void employeeAllRemovedAndNotifiedOnce() {
            registered(diego);
            registered(diego);

            service.removeForEmployee(EMPLOYEE_ID, null);

            assertThat(storedCredentials).isEmpty();
            verify(notifications).notify(eq("diego"), eq(NotificationType.GERAL), anyString(),
                    contains("dos seus 2 aparelhos"), eq("/perfil"));
        }

        @Test
        @DisplayName("o RH remove um, e o aviso diz qual")
        void employeeOneRemoved() {
            registered(diego);
            registered(diego);
            UUID one = storedCredentials.get(0).getId();

            service.removeForEmployee(EMPLOYEE_ID, one);

            assertThat(storedCredentials).hasSize(1);
            verify(notifications).notify(eq("diego"), any(), anyString(), contains("do aparelho Android · Chrome"), anyString());
        }

        @Test
        @DisplayName("id que não é do funcionário: 404, e ninguém é avisado")
        void employeeUnknownCredentialIsNotFound() {
            registered(ana);
            UUID anasDevice = storedCredentials.get(0).getId();

            assertThatThrownBy(() -> service.removeForEmployee(EMPLOYEE_ID, anasDevice))
                    .isInstanceOf(WebAuthnCredentialNotFoundException.class);
            verifyNoInteractions(notifications);
        }

        @Test
        @DisplayName("funcionário sem usuário não tem aparelho: lista vazia")
        void employeeWithoutUserHasNoDevices() {
            assertThat(service.listForEmployee(UUID.randomUUID())).isEmpty();
        }
    }

    // ── Apoio ────────────────────────────────────────────────────────────────

    FakeAuthenticator registered(User user) {
        FakeAuthenticator phone = new FakeAuthenticator();
        service.register(user.getLogin(), phone.register(service.registrationOptions(user.getLogin()), ORIGIN), ANDROID);
        return phone;
    }

    static String handle(User user) {
        return Base64UrlUtil.encodeToString(user.getId().getBytes(StandardCharsets.UTF_8));
    }

    static User user(String id, String login) {
        User user = new User(login, login + "@teste.com", "hash", List.of(UserRole.USER));
        user.setId(id);
        return user;
    }

    /** Os repositórios guardando em memória: o que o serviço grava, ele lê de volta. */
    void fakeDatabase() {
        when(challengeRepository.save(any())).thenAnswer(inv -> {
            WebAuthnChallenge c = inv.getArgument(0);
            if (c.id == null) c.id = UUID.randomUUID();
            storedChallenges.put(c.id, c);
            return c;
        });
        when(challengeRepository.findForUpdate(any())).thenAnswer(inv -> Optional.ofNullable(storedChallenges.get(inv.getArgument(0))));

        when(credentials.save(any())).thenAnswer(inv -> {
            WebAuthnCredential c = inv.getArgument(0);
            if (c.id == null) {
                c.id = UUID.randomUUID();
                storedCredentials.add(c);
            }
            return c;
        });
        when(credentials.existsByCredentialId(anyString())).thenAnswer(inv ->
                storedCredentials.stream().anyMatch(c -> c.getCredentialId().equals(inv.getArgument(0))));
        when(credentials.findByCredentialId(anyString())).thenAnswer(inv ->
                storedCredentials.stream().filter(c -> c.getCredentialId().equals(inv.getArgument(0))).findFirst());
        when(credentials.findByUserIdOrderByCreatedAtDesc(anyString())).thenAnswer(inv ->
                storedCredentials.stream().filter(c -> c.getUserId().equals(inv.getArgument(0))).toList());
        when(credentials.findById(any())).thenAnswer(inv ->
                storedCredentials.stream().filter(c -> c.getId().equals(inv.getArgument(0))).findFirst());
        doAnswer(inv -> storedCredentials.remove((WebAuthnCredential) inv.getArgument(0))).when(credentials).delete(any());
        doAnswer(inv -> storedCredentials.removeAll(inv.<List<WebAuthnCredential>>getArgument(0))).when(credentials).deleteAll(anyList());

        when(users.findByLoginWithEmployee("diego")).thenReturn(Optional.of(diego));
        when(users.findByLoginWithEmployee("ana")).thenReturn(Optional.of(ana));
        when(users.findById("u-diego")).thenReturn(Optional.of(diego));
        when(users.findById("u-ana")).thenReturn(Optional.of(ana));
        when(users.findByEmployee_Id(EMPLOYEE_ID)).thenReturn(Optional.of(diego));
    }

    /** Relógio que o teste empurra: para ver o desafio vencer sem esperar 5 minutos. */
    static final class MovableClock extends Clock {
        private Instant now;

        MovableClock(Instant start) {
            this.now = start;
        }

        void advance(Duration d) {
            now = now.plus(d);
        }

        @Override
        public ZoneId getZone() {
            return ZoneId.of("America/Sao_Paulo");
        }

        @Override
        public Clock withZone(ZoneId zone) {
            return this;
        }

        @Override
        public Instant instant() {
            return now;
        }
    }
}
