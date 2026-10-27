package org.stockbridge.inventory.application.service;

import org.stockbridge.inventory.application.port.in.AuthUseCase;
import org.stockbridge.inventory.application.port.out.PasswordHasherPort;
import org.stockbridge.inventory.application.port.out.UserRepositoryPort;
import org.stockbridge.inventory.domain.exception.EmailAlreadyExistsException;
import org.stockbridge.inventory.domain.exception.InvalidCredentialsException;
import org.stockbridge.inventory.domain.exception.InvalidRequestDataException;
import org.stockbridge.inventory.domain.model.Role;
import org.stockbridge.inventory.domain.model.User;

import java.util.Locale;
import java.util.Objects;
import java.util.regex.Pattern;
import java.nio.charset.StandardCharsets;

public final class AuthUseCaseImpl implements AuthUseCase {
    private static final Pattern EMAIL_PATTERN = Pattern.compile("^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$");

    private final UserRepositoryPort userRepository;
    private final PasswordHasherPort passwordHasher;

    public AuthUseCaseImpl(UserRepositoryPort userRepository, PasswordHasherPort passwordHasher) {
        this.userRepository = Objects.requireNonNull(userRepository, "El repositorio de usuarios es obligatorio.");
        this.passwordHasher = Objects.requireNonNull(passwordHasher, "El servicio de contraseñas es obligatorio.");
    }

    @Override
    public UserSummary register(RegisterCommand command) {
        if (command == null) {
            throw new InvalidRequestDataException("Los datos de registro son obligatorios.");
        }
        String name = command.name() == null ? "" : command.name().trim();
        String email = normalizeEmail(command.email());
        String password = command.password();
        if (name.isBlank() || !EMAIL_PATTERN.matcher(email).matches() || password == null
                || password.length() < 8 || password.getBytes(StandardCharsets.UTF_8).length > 72) {
            throw new InvalidRequestDataException("Se requiere un nombre, un correo válido y una contraseña de 8 a 72 bytes UTF-8.");
        }
        if (userRepository.existsByEmail(email)) {
            throw new EmailAlreadyExistsException("Ya existe una cuenta registrada con este correo.");
        }

        User user = userRepository.save(new User(null, name, email, passwordHasher.hash(password), Role.ROLE_EMPLEADO));
        return toSummary(user);
    }

    @Override
    public UserSummary login(LoginCommand command) {
        if (command == null || command.email() == null || command.password() == null) {
            throw new InvalidCredentialsException("El correo o la contraseña no son válidos.");
        }
        User user = userRepository.findByEmail(normalizeEmail(command.email()))
                .filter(candidate -> passwordHasher.matches(command.password(), candidate.passwordHash()))
                .orElseThrow(() -> new InvalidCredentialsException("El correo o la contraseña no son válidos."));
        return toSummary(user);
    }

    private static UserSummary toSummary(User user) {
        return new UserSummary(user.id(), user.name(), user.email(), user.role());
    }

    private static String normalizeEmail(String email) {
        return email == null ? "" : email.trim().toLowerCase(Locale.ROOT);
    }
}
