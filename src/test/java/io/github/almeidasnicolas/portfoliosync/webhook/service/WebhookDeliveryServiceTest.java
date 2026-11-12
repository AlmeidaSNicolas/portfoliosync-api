package io.github.almeidasnicolas.portfoliosync.webhook.service;

import io.github.almeidasnicolas.portfoliosync.TestcontainersConfiguration;
import io.github.almeidasnicolas.portfoliosync.webhook.domain.DeliveryStatus;
import io.github.almeidasnicolas.portfoliosync.webhook.domain.WebhookDelivery;
import io.github.almeidasnicolas.portfoliosync.webhook.exception.WebhookDeliveryNotFoundException;
import io.github.almeidasnicolas.portfoliosync.webhook.repository.WebhookDeliveryRepository;
import org.junit.jupiter.api.BeforeEach;
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

    @BeforeEach
    void cleanDatabase() {
        webhookDeliveryRepository.deleteAll();
    }

    @Test
    void shouldRegisterSameDeliveryOnlyOnce(){
        UUID randomUUID = UUID.randomUUID();
        assertTrue(webhookDeliveryService.registerIfNew(randomUUID, "push"));
        assertFalse(webhookDeliveryService.registerIfNew(randomUUID, "push"));
        assertEquals(1,webhookDeliveryRepository.count());
    }

    @Test
    void shouldStartAsReceivedWithoutProcessedAt() {
        UUID deliveryId = UUID.randomUUID();

        webhookDeliveryService.registerIfNew(deliveryId, "push");

        WebhookDelivery delivery = find(deliveryId);
        assertEquals(DeliveryStatus.RECEIVED, delivery.getStatus());
        assertNull(delivery.getProcessedAt());
    }

    @Test
    void shouldPersistProcessedStatus() {
        UUID deliveryId = UUID.randomUUID();
        webhookDeliveryService.registerIfNew(deliveryId, "push");

        webhookDeliveryService.markProcessed(deliveryId);

        WebhookDelivery delivery = find(deliveryId);
        assertEquals(DeliveryStatus.PROCESSED, delivery.getStatus());
        assertNotNull(delivery.getProcessedAt());
        assertNull(delivery.getErrorMessage());
    }

    @Test
    void shouldPersistIgnoredStatus() {
        UUID deliveryId = UUID.randomUUID();
        webhookDeliveryService.registerIfNew(deliveryId, "ping");

        webhookDeliveryService.markIgnored(deliveryId);

        WebhookDelivery delivery = find(deliveryId);
        assertEquals(DeliveryStatus.IGNORED, delivery.getStatus());
        assertNotNull(delivery.getProcessedAt());
    }

    @Test
    void shouldPersistFailedStatusWithErrorMessage() {
        UUID deliveryId = UUID.randomUUID();
        webhookDeliveryService.registerIfNew(deliveryId, "push");

        webhookDeliveryService.markFailed(deliveryId, "repo not found");

        WebhookDelivery delivery = find(deliveryId);
        assertEquals(DeliveryStatus.FAILED, delivery.getStatus());
        assertEquals("repo not found", delivery.getErrorMessage());
        assertNotNull(delivery.getProcessedAt());
    }

    @Test
    void shouldPersistRepositoryWithoutChangingStatus() {
        UUID deliveryId = UUID.randomUUID();
        webhookDeliveryService.registerIfNew(deliveryId, "push");

        webhookDeliveryService.recordRepository(deliveryId, 123L, "meu-projeto");

        WebhookDelivery delivery = find(deliveryId);
        assertEquals(123L, delivery.getGithubRepoId());
        assertEquals("meu-projeto", delivery.getRepoName());
        assertEquals(DeliveryStatus.RECEIVED, delivery.getStatus());
        assertNull(delivery.getProcessedAt());
    }

    @Test
    void shouldThrowWhenDeliveryDoesNotExist() {
        UUID unknown = UUID.randomUUID();

        assertThrows(WebhookDeliveryNotFoundException.class, () -> webhookDeliveryService.markProcessed(unknown));
        assertThrows(WebhookDeliveryNotFoundException.class, () -> webhookDeliveryService.markIgnored(unknown));
        assertThrows(WebhookDeliveryNotFoundException.class, () -> webhookDeliveryService.markFailed(unknown, "x"));
        assertThrows(WebhookDeliveryNotFoundException.class, () -> webhookDeliveryService.recordRepository(unknown, 1L, "x"));
    }

    private WebhookDelivery find(UUID deliveryId) {
        return webhookDeliveryRepository.findByDeliveryId(deliveryId).orElseThrow();
    }

}
