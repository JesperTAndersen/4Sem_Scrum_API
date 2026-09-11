package app.dtos.user;

public record CreateUserRequestDTO(
    String firstName,
    String lastName,
    String email,
    String password
) {}
