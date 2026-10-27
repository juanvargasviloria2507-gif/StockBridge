package org.stockbridge.inventory.infrastructure.adapters.out.persistence.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;

@Entity
@Table(name = "requests")
@Getter
@Setter
@NoArgsConstructor
public class RequestEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "product_code", nullable = false, length = 80)
    private String productCode;

    @Column(name = "requester_email", nullable = false, length = 254)
    private String requesterEmail;

    @Column(nullable = false)
    private int quantity;

    @Column(nullable = false, length = 30)
    private String status;

    @Column(name = "fulfillment_source", nullable = false, length = 30)
    private String fulfillmentSource;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "estimated_delivery_days")
    private Integer estimatedDeliveryDays;

    @Column(name = "decision_reason", length = 500)
    private String decisionReason;
}
