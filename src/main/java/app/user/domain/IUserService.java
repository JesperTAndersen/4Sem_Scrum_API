package app.user.domain;

import app.security.presentation.dto.AuthenticatedUser;
import app.user.presentation.dto.*;

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
