package io.github.almeidasnicolas.portfoliosync.webhook.repository;

import io.github.almeidasnicolas.portfoliosync.webhook.domain.WebhookDelivery;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface WebhookDeliveryRepository extends JpaRepository<WebhookDelivery,Long> {

    Optional<WebhookDelivery> findByDeliveryId(UUID deliveryId);

}
