package io.github.almeidasnicolas.portfoliosync.github.exception;

public class GithubListingException extends RuntimeException {
    public GithubListingException(String message) {
        super(message);
    }

    public GithubListingException(String message, Throwable cause){
        super(message, cause);
    }
}
