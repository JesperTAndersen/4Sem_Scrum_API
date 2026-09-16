package app.user.presentation.dto;

import app.security.domain.Role;
import com.fasterxml.jackson.annotation.JsonFormat;

import java.time.LocalDateTime;

public record UserDTO(
    Long id,
    String firstName,
    String lastName,
    String email,
    Role role,
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm")
    LocalDateTime createdAt
)
{
}
