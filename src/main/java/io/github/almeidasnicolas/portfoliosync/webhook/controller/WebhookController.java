package io.github.almeidasnicolas.portfoliosync.webhook.controller;


import io.github.almeidasnicolas.portfoliosync.sync.service.SyncService;
import io.github.almeidasnicolas.portfoliosync.webhook.dto.GithubPushBody;
import io.github.almeidasnicolas.portfoliosync.webhook.service.GithubPushPayloadParser;
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
    private final GithubPushPayloadParser payloadParser;
    private final SyncService syncService;

    public WebhookController(HmacValidator hmacValidator, @Value(value="${github.webhook.secret}") String key, WebhookDeliveryService webhookDeliveryService, GithubPushPayloadParser payloadParser, SyncService syncService){
        this.hmacValidator = hmacValidator;
        this.secretKey = key;
        this.webhookDeliveryService = webhookDeliveryService;
        this.payloadParser = payloadParser;
        this.syncService = syncService;
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

        boolean isNewDelivery = webhookDeliveryService.registerIfNew(deliveryId, event);
        if (!isNewDelivery){
            return ResponseEntity.ok().build();
        }


        if (!"push".equals(event)){
            webhookDeliveryService.markIgnored(deliveryId);
            return ResponseEntity.ok().build();
        }

        try{
            GithubPushBody githubPushBody = payloadParser.parse(body);
            webhookDeliveryService.recordRepository(deliveryId, githubPushBody.repository().id(), githubPushBody.repository().name() );
            syncService.sync(githubPushBody.repository().name());
            webhookDeliveryService.markProcessed(deliveryId);
        }catch (RuntimeException e){
            webhookDeliveryService.markFailed(deliveryId, e.getMessage());
            throw e;
        }


        return ResponseEntity.ok().build();

    }
}
