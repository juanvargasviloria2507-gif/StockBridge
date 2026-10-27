package org.stockbridge.inventory.domain.model;

import java.util.Objects;
import java.util.Locale;

public record User(Long id, String name, String email, String passwordHash, Role role) {
    public User {
        name = Objects.requireNonNull(name, "El nombre del usuario es obligatorio.").trim();
        email = Objects.requireNonNull(email, "El correo del usuario es obligatorio.").trim().toLowerCase(Locale.ROOT);
        passwordHash = Objects.requireNonNull(passwordHash, "El hash de la contraseña es obligatorio.");
        role = Objects.requireNonNull(role, "El rol del usuario es obligatorio.");
    }
}
