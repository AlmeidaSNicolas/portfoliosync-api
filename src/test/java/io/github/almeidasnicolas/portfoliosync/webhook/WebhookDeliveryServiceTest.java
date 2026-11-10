package io.github.almeidasnicolas.portfoliosync.webhook;

import io.github.almeidasnicolas.portfoliosync.TestcontainersConfiguration;
import io.github.almeidasnicolas.portfoliosync.webhook.repository.WebhookDeliveryRepository;
import io.github.almeidasnicolas.portfoliosync.webhook.service.WebhookDeliveryService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@Import(TestcontainersConfiguration.class)
@SpringBootTest(properties = "github.webhook.secret=qualquerTeste")
public class WebhookDeliveryServiceTest {

    @Autowired
    private WebhookDeliveryService webhookDeliveryService;
    @Autowired
    private WebhookDeliveryRepository webhookDeliveryRepository;

    @Test
    void shouldRegisterSameDeliveryOnlyOnce(){
        UUID randomUUID = UUID.randomUUID();
        assertTrue(webhookDeliveryService.registerIfNew(randomUUID, "push"));
        assertFalse(webhookDeliveryService.registerIfNew(randomUUID, "push"));
        assertEquals(1,webhookDeliveryRepository.count());
    }

}
