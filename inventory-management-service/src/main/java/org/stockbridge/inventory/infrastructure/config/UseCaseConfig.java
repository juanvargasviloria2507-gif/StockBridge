package org.stockbridge.inventory.infrastructure.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.stockbridge.inventory.application.port.out.PasswordHasherPort;
import org.stockbridge.inventory.application.port.out.ProductRepositoryPort;
import org.stockbridge.inventory.application.port.out.RequestRepositoryPort;
import org.stockbridge.inventory.application.port.out.SupplierAvailabilityPort;
import org.stockbridge.inventory.application.port.out.UserRepositoryPort;
import org.stockbridge.inventory.application.service.AuthUseCaseImpl;
import org.stockbridge.inventory.application.service.ProductUseCaseImpl;
import org.stockbridge.inventory.application.service.RequestUseCaseImpl;

import java.time.Clock;

@Configuration
public class UseCaseConfig {
    @Bean
    public AuthUseCaseImpl authUseCase(UserRepositoryPort userRepository, PasswordHasherPort passwordHasher) {
        return new AuthUseCaseImpl(userRepository, passwordHasher);
    }

    @Bean
    public ProductUseCaseImpl productUseCase(ProductRepositoryPort productRepository) {
        return new ProductUseCaseImpl(productRepository);
    }

    @Bean
    public RequestUseCaseImpl requestUseCase(ProductRepositoryPort productRepository,
                                             RequestRepositoryPort requestRepository,
                                             SupplierAvailabilityPort supplierAvailability) {
        return new RequestUseCaseImpl(productRepository, requestRepository, supplierAvailability, Clock.systemUTC());
    }
}
