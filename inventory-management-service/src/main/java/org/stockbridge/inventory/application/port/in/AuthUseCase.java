package org.stockbridge.inventory.application.port.in;

import org.stockbridge.inventory.domain.model.Role;

public interface AuthUseCase {
    UserSummary register(RegisterCommand command);

    UserSummary login(LoginCommand command);

    record RegisterCommand(String name, String email, String password) { }

    record LoginCommand(String email, String password) { }

    record UserSummary(Long id, String name, String email, Role role) { }
}
