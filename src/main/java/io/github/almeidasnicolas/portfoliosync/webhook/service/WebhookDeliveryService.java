package io.github.almeidasnicolas.portfoliosync.webhook.service;

import io.github.almeidasnicolas.portfoliosync.webhook.domain.WebhookDelivery;
import io.github.almeidasnicolas.portfoliosync.webhook.repository.WebhookDeliveryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;

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

}


