package app.dtos.security;

import app.enums.UserRole;

public record AuthenticatedUser(
        Long userId,
        String email,
        UserRole userRole
)
{
}
