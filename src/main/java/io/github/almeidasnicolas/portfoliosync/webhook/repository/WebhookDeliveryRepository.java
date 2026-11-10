package io.github.almeidasnicolas.portfoliosync.webhook.repository;

import io.github.almeidasnicolas.portfoliosync.webhook.domain.WebhookDelivery;
import org.springframework.data.jpa.repository.JpaRepository;

public interface WebhookDeliveryRepository extends JpaRepository<WebhookDelivery,Long> {
}
