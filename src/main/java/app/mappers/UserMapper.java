package app.mappers;

import app.dtos.user.UserDTO;
import app.dtos.user.UserReferenceDTO;
import app.entities.User;

public class UserMapper
{
    private UserMapper() {}

    public static UserDTO toDTO(User user)
    {
        return new UserDTO(
            user.getId(),
            user.getFirstName(),
            user.getLastName(),
            user.getEmail(),
            user.getUserRole(),
            user.getCreatedAt()
        );
    }

    public static UserReferenceDTO toReferenceDTO(User user)
    {
        if (user == null)
        {
            return null;
        }

        return new UserReferenceDTO(
            user.getId(),
            user.getFirstName(),
            user.getLastName()
        );
    }
}
