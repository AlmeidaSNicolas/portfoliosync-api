package io.github.almeidasnicolas.portfoliosync.webhook.controller;

import io.github.almeidasnicolas.portfoliosync.webhook.service.HmacValidator;
import io.github.almeidasnicolas.portfoliosync.webhook.service.WebhookDeliveryService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/webhooks/github")
public class WebhookController {
    private final HmacValidator hmacValidator;
    private final String secretKey;
    private final WebhookDeliveryService webhookDeliveryService;



    public WebhookController(HmacValidator hmacValidator, @Value(value="${github.webhook.secret}") String key, WebhookDeliveryService webhookDeliveryService){
        this.hmacValidator = hmacValidator;
        this.secretKey = key;
        this.webhookDeliveryService = webhookDeliveryService;
    }

    @PostMapping
    public ResponseEntity<Void> receiveDelivery(@RequestBody byte[] body,
                                                @RequestHeader(value = "X-Hub-Signature-256", required = false)String signature,
                                                @RequestHeader("X-GitHub-Event")String event,
                                                @RequestHeader("X-GitHub-Delivery")UUID deliveryId
                                                ){

        boolean authentic = hmacValidator.isAuthentic(secretKey, body, signature);
        if (!authentic){
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        webhookDeliveryService.registerIfNew(deliveryId, event);
        return ResponseEntity.ok().build();

    }
}
