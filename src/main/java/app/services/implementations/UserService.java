package app.services.implementations;

import app.dtos.security.AuthenticatedUser;
import app.dtos.user.*;
import app.enums.UserRole;
import app.exceptions.ConflictException;
import app.exceptions.UnauthorizedActionException;
import app.exceptions.ValidationException;
import app.mappers.UserMapper;
import app.persistence.interfaces.specific.IUserDAO;
import app.entities.User;
import app.services.interfaces.IUserService;
import app.utils.PasswordUtil;
import app.utils.ValidationUtil;

import java.util.Comparator;
import java.util.List;
import java.util.Optional;

public class UserService implements IUserService
{
    private final IUserDAO userDAO;
    private static final int BCRYPT_COST = 12;

    public UserService(IUserDAO userDAO)
    {
        this.userDAO = userDAO;
    }

    @Override
    public UserDTO registerUser(CreateUserRequestDTO dto)
    {
        validateCreateInput(dto);
        requireUniqueEmail(dto.email());

        String hashedPassword = PasswordUtil.hashPassword(dto.password(), BCRYPT_COST);

        User user = new User(
            dto.firstName(),
            dto.lastName(),
            dto.email(),
            hashedPassword,
            UserRole.EMPLOYEE
        );

        User created = userDAO.create(user);
        return UserMapper.toDTO(created);
    }

    @Override
    public UserDTO findById(Long id)
    {
        ValidationUtil.validateId(id);

        User user = userDAO.get(id);
        return UserMapper.toDTO(user);
    }

    @Override
    public List<UserDTO> findAll()
    {
        return userDAO.getAll().stream()
            .map(UserMapper::toDTO)
            .sorted(Comparator.comparing(UserDTO::firstName))
            .toList();
    }

    @Override
    public UserDTO update(AuthenticatedUser authUser, Long targetUserId, UpdateUserDTO dto)
    {
        validateUpdateRequest(authUser, targetUserId, dto);
        validateOwnershipOrAdmin(authUser, targetUserId);

        User user = userDAO.get(targetUserId);

        user.update(
            dto.firstName(),
            dto.lastName()
        );

        User updated = userDAO.update(user);
        return UserMapper.toDTO(updated);
    }

    @Override
    public UserDTO changeRole(Long targetUserId, UserRoleUpdateDTO dto)
    {
        ValidationUtil.validateId(targetUserId);
        ValidationUtil.validateNotNull(dto, "User Role");

        User targetUser = userDAO.get(targetUserId);
        targetUser.changeRole(dto.userRole());

        User updated = userDAO.update(targetUser);
        return UserMapper.toDTO(updated);
    }

    @Override
    public UserDTO changeEmail(AuthenticatedUser authUser, Long targetUserId, EmailUpdateDTO dto)
    {
        validateEmailRequest(dto);
        validateOwnershipOrAdmin(authUser, targetUserId);

        User user = userDAO.get(targetUserId);
        user.changeEmail(dto.email());

        User updated = userDAO.update(user);
        return UserMapper.toDTO(updated);
    }

    @Override
    public UserDTO changePassword(AuthenticatedUser authUser, Long targetUserId, ChangeUserPasswordDTO dto)
    {
        validatePassword(dto.newPassword());
        validateOwnershipOrAdmin(authUser, targetUserId);

        User user = userDAO.get(targetUserId);

        if (!user.verifyPassword(dto.currentPassword()))
        {
            throw new ValidationException("Current password is incorrect");
        }

        String hashed = PasswordUtil.hashPassword(dto.newPassword(), BCRYPT_COST);
        user.changePassword(hashed);

        User updated = userDAO.update(user);
        return UserMapper.toDTO(updated);
    }

    @Override
    public boolean delete(AuthenticatedUser authUser, Long targetUserId)
    {
        ValidationUtil.validateNotNull(authUser, "Authenticated User");
        ValidationUtil.validateId(targetUserId);

        if (authUser.userId().equals(targetUserId))
        {
            throw new IllegalArgumentException("Cannot delete your own account");
        }

        return userDAO.delete(targetUserId);
    }

    private void validateOwnershipOrAdmin(AuthenticatedUser authUser, Long targetUserId) {
        ValidationUtil.validateNotNull(authUser, "Authenticated User");

        boolean isOwner = authUser.userId().equals(targetUserId);

        if (!isOwner)
        {
            throw new UnauthorizedActionException("You can only modify your own data");
        }
    }

    private void validateCreateInput(CreateUserRequestDTO dto)
    {
        ValidationUtil.validateNotNull(dto, "User");
        requireMinimumLength(dto.firstName(), "First name");
        requireMinimumLength(dto.lastName(), "Last name");
        validatePassword(dto.password());
    }

    private void validateEmailRequest(EmailUpdateDTO dto)
    {
        ValidationUtil.validateNotNull(dto, "Email");
        ValidationUtil.validateEmail(dto.email());
        requireUniqueEmail(dto.email());
    }

    private void validateUpdateRequest(AuthenticatedUser authUser, Long targetUserId, UpdateUserDTO dto)
    {
        ValidationUtil.validateNotNull(authUser, "Authenticated User");
        ValidationUtil.validateId(targetUserId);
        ValidationUtil.validateId(dto.stationId());
        requireMinimumLength(dto.firstName(), "First name");
        requireMinimumLength(dto.lastName(), "Last name");

        if (!authUser.userId().equals(targetUserId))
        {
            throw new UnauthorizedActionException("You can only update your own profile");
        }
    }

    private void requireUniqueEmail(String email)
    {
        Optional<User> user = userDAO.findByEmail(email);

        if (user.isPresent())
        {
            throw new ConflictException("A user with this email already exists");
        }
    }

    private void validatePassword(String password)
    {
        if (password == null || password.length() < 8)
        {
            throw new ValidationException("Password must be at least 8 characters");
        }

        if (!password.matches(".*[A-Z].*"))
        {
            throw new ValidationException("Password must contain at least one uppercase letter");
        }

        if (!password.matches(".*[0-9].*"))
        {
            throw new ValidationException("Password must contain at least one number");
        }
    }

    private void requireMinimumLength(String name, String field)
    {
        if (name == null || name.trim().isEmpty())
        {
            throw new IllegalArgumentException(field + " cannot be empty");
        }

        if (name.length() < 2)
        {
            throw new ValidationException(field + " must be at least 2 characters");
        }
    }
}


