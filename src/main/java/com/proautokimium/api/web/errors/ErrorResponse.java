package com.proautokimium.api.web.errors;

import java.time.LocalDateTime;

public record ErrorResponse(
        int status,
        String message,
        String path,
        LocalDateTime timestamp
) {

    public static ErrorResponse of (int status, String message, String path){
        return new ErrorResponse(status, message, path, LocalDateTime.now());
    }
}
