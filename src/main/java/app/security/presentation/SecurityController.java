package app.security.presentation;

import app.security.presentation.ISecurityController;
import app.exceptions.ForbiddenException;
import app.exceptions.TokenVerificationException;
import app.exceptions.UnauthorizedException;
import app.security.presentation.dto.AuthenticatedUser;
import app.security.presentation.dto.LoginRequestDTO;
import app.security.presentation.dto.LoginResponseDTO;
import app.user.presentation.dto.CreateUserRequestDTO;
import app.security.domain.ISecurityService;
import app.utils.JWTUtil;
import io.javalin.http.Context;
import io.javalin.http.HandlerType;
import io.javalin.http.HttpStatus;

import java.util.Set;
import java.util.stream.Collectors;

public class SecurityController implements ISecurityController
{
    private final ISecurityService securityService;
    private final String USER_ATTRIBUTE = "authUser";

    public SecurityController(ISecurityService securityService)
    {
        this.securityService = securityService;
    }

    @Override
    public void login(Context ctx)
    {
        LoginRequestDTO request = ctx.bodyAsClass(LoginRequestDTO.class);
        String token = securityService.login(request);
        ctx.status(HttpStatus.OK).json(new LoginResponseDTO(token));
    }

    @Override
    public void register(Context ctx)
    {
        CreateUserRequestDTO request = ctx.bodyAsClass(CreateUserRequestDTO.class);
        AuthenticatedUser user = securityService.register(request);
        ctx.status(HttpStatus.CREATED).json(user);
    }

    @Override
    public void authenticate(Context ctx)
    {
        // CORS preflight request
        if (ctx.method() == HandlerType.OPTIONS)
        {
            return;
        }

        // Endpoint open to ANYONE
        if (isOpenEndpoint(ctx))
        {
            return;
        }

        String token = extractToken(ctx);
        try
        {
            AuthenticatedUser user = JWTUtil.parseToken(token);
            ctx.attribute(USER_ATTRIBUTE, user);
        }
        catch (TokenVerificationException e)
        {
            throw new UnauthorizedException("Invalid or expired token", e);
        }
    }

    @Override
    public void authorize(Context ctx)
    {
        // Endpoint open to ANYONE
        if (isOpenEndpoint(ctx))
        {
            return;
        }

        AuthenticatedUser user = ctx.attribute(USER_ATTRIBUTE);
        validateUser(user);

        if (ctx.routeRoles().isEmpty())
        {
            return;
        }

        validateRole(ctx, user);
    }

    private boolean isOpenEndpoint(Context ctx)
    {
        Set<String> allowedRoles = ctx.routeRoles().stream()
                .map(role -> role.toString().toUpperCase())
                .collect(Collectors.toSet());

        return allowedRoles.contains("ANYONE");
    }

    private String extractToken(Context ctx)
    {
        String header = ctx.header("Authorization");
        if (header == null)
        {
            throw new UnauthorizedException("Authorization header is missing");
        }

        String[] parts = header.split(" ");
        if (parts.length < 2 || parts[1].isBlank())
        {
            throw new UnauthorizedException("Authorization header is malformed");
        }

        return parts[1];
    }

    private void validateUser(AuthenticatedUser user)
    {
        if (user == null)
        {
            throw new UnauthorizedException("No user found on request context");
        }
    }

    private void validateRole(Context ctx, AuthenticatedUser user)
    {
        if (ctx.routeRoles().stream().noneMatch(role -> role.equals(user.role())))
        {
            throw new ForbiddenException("Access denied: required role is " + ctx.routeRoles());
        }
    }
}
