package com.proautokimium.api.web.errors;

import com.proautokimium.api.domain.exceptions.DomainException;
import com.proautokimium.api.domain.exceptions.machine.MachineAlreadyExistsException;
import com.proautokimium.api.domain.exceptions.machine.MachineNotFoundException;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;



@RestControllerAdvice
public class MachineExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(MachineExceptionHandler.class);

    @ExceptionHandler(DomainException.class)
    public ResponseEntity<ErrorResponse> handleMachine(
            DomainException ex,
            HttpServletRequest request
    ){
        HttpStatus status = switch(ex){
            case MachineNotFoundException e -> ex.getStatus();
            case MachineAlreadyExistsException e -> ex.getStatus();

            default -> HttpStatus.BAD_REQUEST;
        };

        log.warn(
                "Machine error | status={} | path={} | msg={}",
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
