package io.github.almeidasnicolas.portfoliosync.webhook.exception;

import java.util.UUID;

public class WebhookDeliveryNotFoundException extends RuntimeException {
    public WebhookDeliveryNotFoundException(UUID deliveryId) {
        super("Webhook delivery not found: " + deliveryId);
    }
}
