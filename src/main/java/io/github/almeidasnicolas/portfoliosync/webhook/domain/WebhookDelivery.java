package io.github.almeidasnicolas.portfoliosync.webhook.domain;

import jakarta.persistence.*;
import lombok.Getter;
import org.hibernate.annotations.CreationTimestamp;

import java.time.Instant;
import java.util.UUID;

@Getter
@Entity
@Table(name = "webhook_deliveries")
public class WebhookDelivery {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private UUID deliveryId;
    private String event;
    private Long githubRepoId;
    private String repoName;

    @Enumerated(EnumType.STRING)
    private DeliveryStatus status;

    private String errorMessage;

    @CreationTimestamp
    @Column(updatable = false)
    private Instant receivedAt;

    private Instant processedAt;

    protected WebhookDelivery() {

    }

    public WebhookDelivery(UUID deliveryId, String event) {
        this.deliveryId = deliveryId;
        this.event = event;
        this.status = DeliveryStatus.RECEIVED;
    }
}
