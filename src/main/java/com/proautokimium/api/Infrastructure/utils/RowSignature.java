package com.proautokimium.api.Infrastructure.utils;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.HexFormat;
import java.util.stream.Collectors;

public final class RowSignature {

    private RowSignature() {}

    private static final String SEPARATOR = "\u001F";

    private static String normalize(String content){
        if (content == null || content.isBlank()){ return null; }

        return content.trim();
    }

    private static String digest(String content){
        try{
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] bytes = md.digest(content.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(bytes).substring(0,12);
        }catch (NoSuchAlgorithmException e){
            throw new IllegalStateException("SHA-256 sempre existe na JVM", e);
        }
    }

    private static String join(String[] parts){
        return Arrays.stream(parts)
                .map(RowSignature::normalize)
                .collect(Collectors.joining(SEPARATOR));
    }

    public static String of(String... parts){
        return digest(join(parts));
    }
}
