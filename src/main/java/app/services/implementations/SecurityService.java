package app.services.implementations;

import app.dtos.security.AuthenticatedUser;
import app.dtos.user.CreateUserRequestDTO;
import app.exceptions.ConflictException;
import app.exceptions.UnauthorizedException;
import app.mappers.UserMapper;
import app.entities.User;
import app.persistence.interfaces.specific.IUserDAO;
import app.dtos.security.LoginRequestDTO;
import app.services.interfaces.ISecurityService;
import app.utils.JWTUtil;
import app.utils.PasswordUtil;
import app.utils.ValidationUtil;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class SecurityService implements ISecurityService
{
    private final IUserDAO userDAO;

    public SecurityService(IUserDAO userDAO)
    {
        this.userDAO = userDAO;
    }

    @Override
    public AuthenticatedUser register(CreateUserRequestDTO request)
    {
        ValidationUtil.validateEmail(request.email());
        ValidationUtil.validatePassword(request.password());
        ValidationUtil.validateNotBlank(request.firstName(), "First name");
        ValidationUtil.validateNotBlank(request.lastName(), "Last name");
        validateUnique(request.email());

        String hashedPassword = PasswordUtil.hashPassword(request.password());
        User user = userDAO.create(new User(request.firstName(),request.lastName(),request.email(),hashedPassword));
        AuthenticatedUser dto = UserMapper.toAuthenticatedUser(user);
        log.info("User registered: {}", user.getEmail());
        return dto;
    }

    @Override
    public String login(LoginRequestDTO request)
    {
        ValidationUtil.validateEmail(request.email());
        ValidationUtil.validateNotBlank(request.password(), "Password");

        User user = userDAO.findByEmail(request.email()).orElseThrow(() ->
        {
            log.warn("Failed login attempt for: {}", request.email());
            return new UnauthorizedException("Invalid credentials");
        });

        if (!PasswordUtil.verifyPassword(request.password(), user.getHashedPassword()))
        {
            log.warn("Failed login attempt for: {}", user.getEmail());
            throw new UnauthorizedException("Invalid credentials");
        }

        rehashIfNeeded(user, request.password());
        String token = JWTUtil.createToken(user.getId(), user.getEmail(), user.getRole());
        log.info("User logged in: {}", user.getEmail());
        return token;
    }

    private void validateUnique(String email)
    {
        if (userDAO.existsByEmail(email))
        {
            throw new ConflictException("The chosen email is not available: " + email);
        }
    }

    private void rehashIfNeeded(User user, String plainPassword)
    {
        if (PasswordUtil.needsRehash(user.getHashedPassword()))
        {
            user.setHashedPassword(PasswordUtil.hashPassword(plainPassword));
            userDAO.update(user);
            log.warn("Password rehashed for user: {}", user.getEmail());
        }
    }
}
