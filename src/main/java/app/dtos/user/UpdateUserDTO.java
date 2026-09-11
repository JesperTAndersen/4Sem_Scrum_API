package app.dtos.user;

public record UpdateUserDTO(
    String firstName,
    String lastName,
    Long stationId
)
{
}
