package org.stockbridge.inventory.infrastructure.observability;

import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class MetricsConfig {
    @Bean
    public StockBridgeMetrics stockBridgeMetrics(MeterRegistry meterRegistry) {
        return new StockBridgeMetrics(
                meterRegistry.counter("stockbridge.requests.created"),
                meterRegistry.counter("stockbridge.supplier.availability.checks"));
    }
}
