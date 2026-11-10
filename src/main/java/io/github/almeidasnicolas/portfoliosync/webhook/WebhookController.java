package io.github.almeidasnicolas.portfoliosync.webhook;

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RequiredArgsConstructor
@RestController
@RequestMapping("/webhooks/github")
public class WebhookController {
    private final HmacValidator hmacValidator;

    @PostMapping
    public ResponseEntity<Void> receiveDelivery(@RequestBody byte[] body,
                                                @RequestHeader(value = "X-Hub-Signature-256", required = false)String signature,
                                                @RequestHeader("X-GitHub-Event")String event ){
        String keyNotSecretTest = "qualquerThing";

        boolean authentic = hmacValidator.isAuthentic(keyNotSecretTest, body, signature);
        if (!authentic){
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        return ResponseEntity.ok().build();

    }
}
