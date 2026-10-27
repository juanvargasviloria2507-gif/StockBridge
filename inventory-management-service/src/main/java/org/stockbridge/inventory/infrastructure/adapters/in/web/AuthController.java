package org.stockbridge.inventory.infrastructure.adapters.in.web;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.stockbridge.inventory.application.port.in.AuthUseCase;
import org.stockbridge.inventory.infrastructure.security.JwtProvider;

@RestController
@RequestMapping("/auth")
public class AuthController {
    private final AuthUseCase authUseCase;
    private final JwtProvider jwtProvider;

    public AuthController(AuthUseCase authUseCase, JwtProvider jwtProvider) {
        this.authUseCase = authUseCase;
        this.jwtProvider = jwtProvider;
    }

    @PostMapping("/register")
    @org.springframework.web.bind.annotation.ResponseStatus(HttpStatus.CREATED)
    public AuthResponse register(@Valid @RequestBody RegisterRequest body) {
        AuthUseCase.UserSummary user = authUseCase.register(
                new AuthUseCase.RegisterCommand(body.name(), body.email(), body.password()));
        return toResponse(user);
    }

    @PostMapping("/login")
    public AuthResponse login(@Valid @RequestBody LoginRequest body) {
        AuthUseCase.UserSummary user = authUseCase.login(new AuthUseCase.LoginCommand(body.email(), body.password()));
        return toResponse(user);
    }

    private AuthResponse toResponse(AuthUseCase.UserSummary user) {
        return new AuthResponse(jwtProvider.generateToken(user.email(), user.role()), user.id(),
                user.name(), user.email(), user.role().name());
    }

    public record RegisterRequest(@NotBlank(message = "El nombre es obligatorio.")
                                  @Size(max = 120, message = "El nombre no puede superar los 120 caracteres.") String name,
                                  @NotBlank(message = "El correo es obligatorio.")
                                  @Email(message = "El correo no tiene un formato válido.")
                                  @Size(max = 254, message = "El correo no puede superar los 254 caracteres.") String email,
                                  @NotBlank(message = "La contraseña es obligatoria.")
                                  @Size(min = 8, max = 72, message = "La contraseña debe tener entre 8 y 72 caracteres.") String password) { }

    public record LoginRequest(@NotBlank(message = "El correo es obligatorio.")
                               @Email(message = "El correo no tiene un formato válido.")
                               @Size(max = 254, message = "El correo no puede superar los 254 caracteres.") String email,
                               @NotBlank(message = "La contraseña es obligatoria.") String password) { }

    public record AuthResponse(String token, Long id, String name, String email, String role) { }
}
