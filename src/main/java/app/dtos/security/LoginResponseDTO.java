package app.dtos.security;

public record LoginResponseDTO(
    String token,
    String email,
    String role
)
{
}
