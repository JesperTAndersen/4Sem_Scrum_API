package app.dtos.security.temp;

import app.enums.Role;

import java.util.Set;

public record UserSecurityDTO(
        Long id,
        String username,
        Set<Role> roles
)
{
}
