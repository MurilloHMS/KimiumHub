package com.proautokimium.api.web.errors;

import com.proautokimium.api.domain.exceptions.DomainException;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class DomainExceptionHandler {

    private final Logger log = LoggerFactory.getLogger(DomainException.class);

    @ExceptionHandler(DomainException.class)
    public ResponseEntity<ErrorResponse> handleDomain(
            DomainException ex,
            HttpServletRequest request
    ){

        log.warn(
                "Domain error | status={} | path={} | msg={}",
                ex.getStatus(),
                request.getRequestURI(),
                ex.getMessage()
        );

        return ResponseEntity.status(ex.getStatus())
                .body(ErrorResponse.of(
                        ex.getStatus().value(),
                        ex.getMessage(),
                        request.getRequestURI()
                ));
    }
}
