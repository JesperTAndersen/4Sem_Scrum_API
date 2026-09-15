package app.dtos.user;

import app.enums.Role;

public record UserRoleUpdateDTO(
    Role role
)
{
}
