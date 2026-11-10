package io.github.almeidasnicolas.portfoliosync.github;

public class RepositoryNotFoundException extends RuntimeException {
    public RepositoryNotFoundException(String message, Throwable cause) {
        super(message, cause);
    }
}
