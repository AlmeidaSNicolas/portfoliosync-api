package io.github.almeidasnicolas.portfoliosync.webhook.service;

import io.github.almeidasnicolas.portfoliosync.webhook.domain.WebhookDelivery;
import io.github.almeidasnicolas.portfoliosync.webhook.exception.WebhookDeliveryNotFoundException;
import io.github.almeidasnicolas.portfoliosync.webhook.repository.WebhookDeliveryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@RequiredArgsConstructor
@Service
public class WebhookDeliveryService {
    private final WebhookDeliveryRepository webhookDeliveryRepository;

    public boolean registerIfNew(UUID deliveryId, String event) {
        WebhookDelivery webhookDelivery = new WebhookDelivery(deliveryId, event);
        try {
            webhookDeliveryRepository.saveAndFlush(webhookDelivery);
            return true;
        } catch (DataIntegrityViolationException e) {
            return false;
        }

    }

    @Transactional
    public void markIgnored(UUID deliveryId) {
        findOrThrow(deliveryId).markIgnored();
    }

    @Transactional
    public void markProcessed(UUID deliveryId) {
        findOrThrow(deliveryId).markProcessed();
    }

    @Transactional
    public void markFailed(UUID deliveryId, String errorMessage) {
        findOrThrow(deliveryId).markFailed(errorMessage);
    }

    @Transactional
    public void recordRepository(UUID deliveryId, Long githubRepoId, String repoName) {
        findOrThrow(deliveryId).recordRepository(githubRepoId, repoName);
    }

    private WebhookDelivery findOrThrow(UUID deliveryId) {
        return webhookDeliveryRepository.findByDeliveryId(deliveryId)
                .orElseThrow(() -> new WebhookDeliveryNotFoundException(deliveryId));
    }

}


