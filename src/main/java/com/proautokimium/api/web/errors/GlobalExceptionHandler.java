package com.proautokimium.api.web.errors;

import com.proautokimium.api.Infrastructure.exceptions.InfrastructureException;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.MessageSourceResolvable;
import org.springframework.context.support.DefaultMessageSourceResolvable;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.HandlerMethodValidationException;

import java.time.LocalDateTime;
import java.util.Objects;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    /** Permissão negada é resposta, não falha: 403, e sem stack trace no log. */
    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ErrorResponse> handleAccessDenied(AccessDeniedException ex, HttpServletRequest request) {
        return build(HttpStatus.FORBIDDEN, ex.getMessage(), request);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleAny(
            Exception ex,
            HttpServletRequest request
    ) {

        log.error("Unhandled error", ex);

        return build(HttpStatus.INTERNAL_SERVER_ERROR, "Erro interno no servidor", request);
    }

    /** Campo recusado pela validação: 400, com a mensagem da anotação. */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidation(MethodArgumentNotValidException ex, HttpServletRequest request) {
        String message = ex.getBindingResult().getFieldErrors().stream()
                .map(DefaultMessageSourceResolvable::getDefaultMessage)
                .findFirst()
                .orElse("Dados inválidos.");

        return build(HttpStatus.BAD_REQUEST, message, request);
    }

    /**
     * Parâmetro obrigatório que não veio: 400, dizendo **qual**.
     *
     * Sem isto cai no `Exception.class` e sai como 500 "Erro interno no
     * servidor" — quem chamou fica sem saber que esqueceu um parâmetro, e quem
     * mantém vai procurar bug onde não tem. Mesma família do incidente de
     * 2026-08-18, quando onze endpoints respondiam 500 para campo em branco.
     */
    @ExceptionHandler(MissingServletRequestParameterException.class)
    public ResponseEntity<ErrorResponse> handleMissingParameter(
            MissingServletRequestParameterException ex, HttpServletRequest request) {
        return build(HttpStatus.BAD_REQUEST,
                "O parâmetro '" + ex.getParameterName() + "' é obrigatório.", request);
    }

    /**
     * Parâmetro com o tipo errado — uma data mal formatada, por exemplo.
     *
     * Também caía no 500. É erro de quem chamou, e a mensagem diz o que se
     * esperava sem vazar o nome da classe Java para a tela.
     */
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ErrorResponse> handleTypeMismatch(
            MethodArgumentTypeMismatchException ex, HttpServletRequest request) {
        return build(HttpStatus.BAD_REQUEST,
                "O valor de '" + ex.getName() + "' não está no formato esperado.", request);
    }

    /**
     * Falha técnica: 503, mensagem genérica, stack trace no log. O que quebrou
     * é assunto de quem mantém o sistema, não de quem usou a tela.
     */
    @ExceptionHandler(InfrastructureException.class)
    public ResponseEntity<ErrorResponse> handleInfrastructure(InfrastructureException ex, HttpServletRequest request) {
        log.error("Falha de infraestrutura em {}", request.getRequestURI(), ex);
        return build(HttpStatus.SERVICE_UNAVAILABLE, "Serviço temporariamente indisponível. Tente novamente em alguns minutos.", request);
    }

    private ResponseEntity<ErrorResponse> build(HttpStatusCode status, String message, HttpServletRequest request) {
        return ResponseEntity.status(status)
                .body(new ErrorResponse(
                        status.value(),
                        message,
                        request.getRequestURI(),
                        LocalDateTime.now()
                ));
    }

    /**
     * Validação que o Spring 6.1+ faz no nível do método, não do corpo.
     *
     * Quando um parâmetro carrega uma constraint direto — o `@NotNull` ao lado
     * do `@RequestBody`, por exemplo — o Spring passa a validar o método
     * inteiro e lança `HandlerMethodValidationException` em vez de
     * `MethodArgumentNotValidException`. São 11 endpoints assim hoje.
     *
     * Sem este handler ela caía no `handleAny` e virava 500 — inclusive no
     * formulário público de contato, onde campo em branco respondia "Erro
     * interno no servidor". A exceção já carrega 400; só faltava alguém ler.
     */
    @ExceptionHandler(HandlerMethodValidationException.class)
    public ResponseEntity<ErrorResponse> handleMethodValidation(HandlerMethodValidationException ex, HttpServletRequest request) {
        String message = ex.getAllErrors().stream()
                .map(MessageSourceResolvable::getDefaultMessage)
                .filter(Objects::nonNull)
                .findFirst()
                .orElse("Dados inválidos.");

        return build(HttpStatus.BAD_REQUEST, message, request);
    }
}