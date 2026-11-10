package io.github.almeidasnicolas.portfoliosync.webhook;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/webhooks/github")
public class WebhookController {
    private final HmacValidator hmacValidator;
    private final String secretKey;

    public WebhookController(HmacValidator hmacValidator, @Value(value="${github.webhook.secret}") String key){
        this.hmacValidator = hmacValidator;
        this.secretKey = key;
    }

    @PostMapping
    public ResponseEntity<Void> receiveDelivery(@RequestBody byte[] body,
                                                @RequestHeader(value = "X-Hub-Signature-256", required = false)String signature,
                                                @RequestHeader("X-GitHub-Event")String event ){

        boolean authentic = hmacValidator.isAuthentic(secretKey, body, signature);
        if (!authentic){
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        return ResponseEntity.ok().build();

    }
}
