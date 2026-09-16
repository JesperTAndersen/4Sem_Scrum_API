package app.user.data;

import app.security.presentation.dto.AuthenticatedUser;
import app.user.presentation.dto.UserDTO;
import app.user.presentation.dto.UserReferenceDTO;
import app.user.domain.User;

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
            user.getRole(),
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

    public static AuthenticatedUser toAuthenticatedUser(User user)
    {
        return new AuthenticatedUser(
                user.getId(),
                user.getEmail(),
                user.getRole()
        );
    }
}
