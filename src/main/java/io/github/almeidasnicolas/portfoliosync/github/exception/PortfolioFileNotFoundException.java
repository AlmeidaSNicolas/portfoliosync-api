package io.github.almeidasnicolas.portfoliosync.github.exception;

public class PortfolioFileNotFoundException extends RuntimeException {
    public PortfolioFileNotFoundException(String message, Throwable cause) {
        super(message, cause);
    }
}
