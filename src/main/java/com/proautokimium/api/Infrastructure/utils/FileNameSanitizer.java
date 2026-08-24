package com.proautokimium.api.Infrastructure.utils;

public final class FileNameSanitizer {

    public static String sanitize(String name){
        return name.replaceAll("[/\\\\:*?\"<>|]", "")
                .replace("..",".")
                .trim();
    }
}
