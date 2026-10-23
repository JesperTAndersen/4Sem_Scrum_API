package app.security.presentation.dto;

public record LoginRequestDTO(
        String email,
        String password
)
{
}
