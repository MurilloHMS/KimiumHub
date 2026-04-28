package com.proautokimium.api.Infrastructure.services.secrets;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.Arrays;
import java.util.Base64;

@Service
public class CryptoService {

    private final SecretKey masterKey;

    public CryptoService(@Value("${app.master-key}") String b64Key){
        byte[] decoded = Base64.getDecoder().decode(b64Key);
        this.masterKey = new SecretKeySpec(decoded, "AES");
    }

    public EncryptedData encrypt(String plainText) throws Exception{
        byte[] iv = new byte[12];
        new SecureRandom().nextBytes(iv);
        GCMParameterSpec spec = new GCMParameterSpec(128, iv);
        Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
        cipher.init(Cipher.ENCRYPT_MODE, masterKey, spec);
        byte[] out = cipher.doFinal(plainText.getBytes(StandardCharsets.UTF_8));
        byte[] content = Arrays.copyOf(out, out.length - 16);
        byte[] tag = Arrays.copyOfRange(out, out.length - 16, out.length);
        return new EncryptedData(content, iv, tag);
    }

    public String decrypt(byte[] cipherText, byte[] iv, byte[] authTag) throws Exception{
        byte[] combined = concat(cipherText, authTag);
        GCMParameterSpec spec = new GCMParameterSpec(128, iv);
        Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
        cipher.init(Cipher.DECRYPT_MODE, masterKey, spec);
        return new String(cipher.doFinal(combined), StandardCharsets.UTF_8);
    }

    private byte[] concat(byte[] a, byte[] b) {
        byte[] result = new byte[a.length + b.length];
        System.arraycopy(a, 0, result, 0, a.length);
        System.arraycopy(b, 0, result, a.length, b.length);
        return result;
    }
}
