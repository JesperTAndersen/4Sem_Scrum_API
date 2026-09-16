package app.security.presentation.dto;

import app.security.domain.Role;

public record AuthenticatedUser(
        Long id,
        String email,
        Role role
)
{
}
