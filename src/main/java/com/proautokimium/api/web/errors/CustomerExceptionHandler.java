package com.proautokimium.api.web.errors;

import com.proautokimium.api.domain.exceptions.DomainException;
import com.proautokimium.api.domain.exceptions.customer.CustomerAlreadyExistsException;
import com.proautokimium.api.domain.exceptions.customer.CustomerNotFoundException;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class CustomerExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(CustomerExceptionHandler.class);

    @ExceptionHandler(DomainException.class)
    public ResponseEntity<ErrorResponse> handleCustomer(
            DomainException ex,
            HttpServletRequest request
    ){
        HttpStatus status = switch (ex) {
            case CustomerNotFoundException e -> HttpStatus.NOT_FOUND;
            case CustomerAlreadyExistsException e -> HttpStatus.CONFLICT;

            default -> HttpStatus.BAD_REQUEST;
        };

        log.warn(
                "Customer error | status={} | path={} | msg={}",
                status.value(),
                request.getRequestURI(),
                ex.getMessage()
        );

        return ResponseEntity.status(status)
                .body(ErrorResponse.of(
                        status.value(),
                        ex.getMessage(),
                        request.getRequestURI()
                ));
    }

}

