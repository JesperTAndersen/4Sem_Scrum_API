package app.services;

import app.dtos.security.AuthenticatedUser;
import app.dtos.user.*;

import java.util.List;

public interface IUserService
{
    UserDTO registerUser(CreateUserRequestDTO dto);

    UserDTO findById(Long userId);

    List<UserDTO> findAll();

    UserDTO update(AuthenticatedUser authUser, Long userId, UpdateUserDTO dto);

    boolean delete(AuthenticatedUser authUser, Long targetUserId);

    UserDTO changeRole(Long targetUserId, UserRoleUpdateDTO dto);

    UserDTO changeEmail(AuthenticatedUser authUser, Long targetUserId, EmailUpdateDTO dto);

    UserDTO changePassword(AuthenticatedUser authUser, Long targetUserId, ChangeUserPasswordDTO dto);

}
