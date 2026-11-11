package io.github.almeidasnicolas.portfoliosync.webhook.exception;

public class InvalidWebhookPayloadFormatException extends RuntimeException {
    public InvalidWebhookPayloadFormatException(String message) {
        super(message);
    }
}
