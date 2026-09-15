package app.dtos.security;

import app.enums.Role;

public record AuthenticatedUser(
        Long id,
        String email,
        Role role
)
{
}
