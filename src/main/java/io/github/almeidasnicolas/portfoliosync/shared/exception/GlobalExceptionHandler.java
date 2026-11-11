package io.github.almeidasnicolas.portfoliosync.shared.exception;

import io.github.almeidasnicolas.portfoliosync.webhook.exception.InvalidWebhookPayloadFormatException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice

public class GlobalExceptionHandler {
    @ExceptionHandler(UnsupportedLanguageException.class)
    public ProblemDetail handleUnsupportedLanguage(UnsupportedLanguageException ex) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, ex.getMessage());
    }

    @ExceptionHandler(InvalidWebhookPayloadFormatException.class)
    public ProblemDetail handleInvalidWebhookPayload(InvalidWebhookPayloadFormatException ex){
        return ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, ex.getMessage());
    }

}
