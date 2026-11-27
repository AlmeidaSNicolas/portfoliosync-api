package io.github.almeidasnicolas.portfoliosync.shared.exception;

import io.github.almeidasnicolas.portfoliosync.github.exception.GithubListingException;
import io.github.almeidasnicolas.portfoliosync.github.exception.InvalidPortfolioFileException;
import io.github.almeidasnicolas.portfoliosync.github.exception.PortfolioFileNotFoundException;
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

    @ExceptionHandler(GithubListingException.class)
    public ProblemDetail handleGithubListingFailure(GithubListingException ex){
        return ProblemDetail.forStatusAndDetail(HttpStatus.BAD_GATEWAY, ex.getMessage());
    }

    // The push is accepted and recorded as FAILED; 500 keeps the failure visible in GitHub's delivery list.
    @ExceptionHandler(PortfolioFileNotFoundException.class)
    public ProblemDetail handlePortfolioFileNotFound(PortfolioFileNotFoundException ex){
        return ProblemDetail.forStatusAndDetail(HttpStatus.INTERNAL_SERVER_ERROR, ex.getMessage());
    }

    @ExceptionHandler(InvalidPortfolioFileException.class)
    public ProblemDetail handleInvalidPortfolioFile(InvalidPortfolioFileException ex){
        return ProblemDetail.forStatusAndDetail(HttpStatus.INTERNAL_SERVER_ERROR, ex.getMessage());
    }

}
