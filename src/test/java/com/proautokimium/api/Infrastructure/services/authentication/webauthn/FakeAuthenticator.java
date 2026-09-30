package com.proautokimium.api.Infrastructure.services.authentication.webauthn;

import com.proautokimium.api.Application.DTOs.webauthn.AuthenticateCredentialDTO;
import com.proautokimium.api.Application.DTOs.webauthn.AuthenticationOptionsDTO;
import com.proautokimium.api.Application.DTOs.webauthn.RegisterCredentialDTO;
import com.proautokimium.api.Application.DTOs.webauthn.RegistrationOptionsDTO;
import com.webauthn4j.converter.AttestationObjectConverter;
import com.webauthn4j.converter.AuthenticatorDataConverter;
import com.webauthn4j.converter.util.ObjectConverter;
import com.webauthn4j.data.attestation.AttestationObject;
import com.webauthn4j.data.attestation.authenticator.AAGUID;
import com.webauthn4j.data.attestation.authenticator.AttestedCredentialData;
import com.webauthn4j.data.attestation.authenticator.AuthenticatorData;
import com.webauthn4j.data.attestation.authenticator.EC2COSEKey;
import com.webauthn4j.data.attestation.statement.COSEAlgorithmIdentifier;
import com.webauthn4j.data.attestation.statement.NoneAttestationStatement;
import com.webauthn4j.data.extension.authenticator.AuthenticationExtensionAuthenticatorOutput;
import com.webauthn4j.data.extension.authenticator.RegistrationExtensionAuthenticatorOutput;
import com.webauthn4j.util.Base64UrlUtil;

import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.security.Signature;
import java.security.interfaces.ECPublicKey;
import java.security.spec.ECGenParameterSpec;
import java.util.List;
import java.util.UUID;

/**
 * Faz o que o leitor de digital do celular faz, com criptografia de verdade:
 * cria o par de chaves, monta os dados na norma e assina com a chave privada.
 *
 * <p>É o que deixa o teste passar pela MESMA verificação que o celular real
 * vai enfrentar — um mock da webauthn4j provaria só que o serviço chama um
 * método, não que a assinatura bate.
 */
final class FakeAuthenticator {

    private static final byte UP = 0x01;   // a pessoa estava lá (tocou)
    private static final byte UV = 0x04;   // e provou quem é (digital ou PIN)
    private static final byte AT = 0x40;   // os dados trazem a chave pública nova

    private final ObjectConverter converter = new ObjectConverter();
    private final KeyPair keyPair;
    final byte[] credentialId = new byte[32];

    /** O que o próximo login manda como contador. */
    long signCount = 0;
    /** Liga/desliga a marca de "provou quem é" (UV): sem ela, a verificação deve recusar. */
    boolean userVerified = true;

    FakeAuthenticator() {
        try {
            KeyPairGenerator generator = KeyPairGenerator.getInstance("EC");
            generator.initialize(new ECGenParameterSpec("secp256r1"));
            this.keyPair = generator.generateKeyPair();
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
        new SecureRandom().nextBytes(credentialId);
    }

    String credentialIdBase64() {
        return Base64UrlUtil.encodeToString(credentialId);
    }

    /** A resposta de {@code navigator.credentials.create()} para estas opções, vinda desta origem. */
    RegisterCredentialDTO register(RegistrationOptionsDTO options, String origin) {
        byte[] clientData = clientData("webauthn.create", options.challenge(), origin);
        AttestedCredentialData credential = new AttestedCredentialData(AAGUID.ZERO, credentialId,
                EC2COSEKey.create((ECPublicKey) keyPair.getPublic(), COSEAlgorithmIdentifier.ES256));
        AuthenticatorData<RegistrationExtensionAuthenticatorOutput> data =
                new AuthenticatorData<>(sha256(options.rpId().getBytes(StandardCharsets.UTF_8)), flags(AT), signCount, credential);
        byte[] attestation = new AttestationObjectConverter(converter)
                .convertToBytes(new AttestationObject(data, new NoneAttestationStatement()));
        return new RegisterCredentialDTO(options.challengeId(), Base64UrlUtil.encodeToString(clientData),
                Base64UrlUtil.encodeToString(attestation), List.of("internal"));
    }

    /** A resposta de {@code navigator.credentials.get()}: assina os dados e o hash do clientData. */
    AuthenticateCredentialDTO login(AuthenticationOptionsDTO options, String origin, String userHandle) {
        return login(options.challengeId(), options.challenge(), options.rpId(), origin, userHandle);
    }

    AuthenticateCredentialDTO login(UUID challengeId, String challenge, String rpId, String origin, String userHandle) {
        byte[] clientData = clientData("webauthn.get", challenge, origin);
        AuthenticatorData<AuthenticationExtensionAuthenticatorOutput> data =
                new AuthenticatorData<>(sha256(rpId.getBytes(StandardCharsets.UTF_8)), flags((byte) 0), signCount);
        byte[] authenticatorData = new AuthenticatorDataConverter(converter).convert(data);
        byte[] signature = sign(authenticatorData, sha256(clientData));
        return new AuthenticateCredentialDTO(challengeId, credentialIdBase64(),
                Base64UrlUtil.encodeToString(clientData), Base64UrlUtil.encodeToString(authenticatorData),
                Base64UrlUtil.encodeToString(signature), userHandle);
    }

    private byte flags(byte extra) {
        return (byte) (UP | (userVerified ? UV : 0) | extra);
    }

    private static byte[] clientData(String type, String challenge, String origin) {
        return ("{\"type\":\"" + type + "\",\"challenge\":\"" + challenge + "\",\"origin\":\"" + origin
                + "\",\"crossOrigin\":false}").getBytes(StandardCharsets.UTF_8);
    }

    private byte[] sign(byte[] authenticatorData, byte[] clientDataHash) {
        try {
            ByteArrayOutputStream signed = new ByteArrayOutputStream();
            signed.write(authenticatorData);
            signed.write(clientDataHash);
            Signature ecdsa = Signature.getInstance("SHA256withECDSA");
            ecdsa.initSign(keyPair.getPrivate());
            ecdsa.update(signed.toByteArray());
            return ecdsa.sign();
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }

    private static byte[] sha256(byte[] value) {
        try {
            return MessageDigest.getInstance("SHA-256").digest(value);
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }
}
