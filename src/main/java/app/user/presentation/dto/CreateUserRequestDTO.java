package app.user.presentation.dto;

public record CreateUserRequestDTO(
    String firstName,
    String lastName,
    String email,
    String password
) {}
