package app.dtos.user;

import app.enums.UserRole;
import com.fasterxml.jackson.annotation.JsonFormat;

import java.time.LocalDateTime;

public record UserDTO(
    Long id,
    String firstName,
    String lastName,
    String email,
    UserRole userRole,
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm")
    LocalDateTime createdAt
)
{
}
