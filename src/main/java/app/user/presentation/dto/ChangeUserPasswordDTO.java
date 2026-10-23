package app.user.presentation.dto;

public record ChangeUserPasswordDTO(
    String currentPassword,
    String newPassword
)
{
}
