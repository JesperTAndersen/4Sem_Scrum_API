package app.user.presentation.dto;

import app.security.domain.Role;

public record UserRoleUpdateDTO(
    Role role
)
{
}
