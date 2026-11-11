package io.github.almeidasnicolas.portfoliosync.github.exception;

public class RepositoryNotFoundException extends RuntimeException {
    public RepositoryNotFoundException(String message, Throwable cause) {
        super(message, cause);
    }
}
