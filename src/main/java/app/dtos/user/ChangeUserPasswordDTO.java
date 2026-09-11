package app.dtos.user;

public record ChangeUserPasswordDTO(
    String currentPassword,
    String newPassword
)
{
}
