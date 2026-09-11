package app.controllers;

import app.dtos.security.AuthenticatedUser;
import app.dtos.user.*;
import app.services.interfaces.IUserService;
import app.utils.RequestUtil;
import app.utils.SecurityUtil;
import io.javalin.http.Context;

import java.util.List;
import java.util.Objects;

public class UserController implements IUserController
{
    private final IUserService userService;

    public UserController(IUserService userService)
    {
        this.userService = userService;
    }

    @Override
    public void changeRole(Context ctx)
    {
        Long targetUserId = RequestUtil.requirePathId(ctx, "id");

        UserRoleUpdateDTO dto = ctx.bodyValidator(UserRoleUpdateDTO.class)
            .check(d -> d.userRole() != null, "Role cannot be null")
            .get();

        UserDTO userDTO = userService.changeRole(targetUserId, dto);
        ctx.status(200).json(userDTO);
    }

    @Override
    public void changeEmail(Context ctx)
    {
        AuthenticatedUser authUser = SecurityUtil.getAuthenticatedUser(ctx);
        Long targetUserId = RequestUtil.requirePathId(ctx, "id");

        EmailUpdateDTO dto = ctx.bodyValidator(EmailUpdateDTO.class)
            .check(d -> d.email() != null && d.email().contains("@"), "Valid email is required")
            .get();

        UserDTO userDTO = userService.changeEmail(authUser, targetUserId, dto);
        ctx.status(200).json(userDTO);
    }

    @Override
    public void changePassword(Context ctx)
    {
        AuthenticatedUser authUser = SecurityUtil.getAuthenticatedUser(ctx);
        Long targetUserId = RequestUtil.requirePathId(ctx, "id");

        ChangeUserPasswordDTO dto = ctx.bodyValidator(ChangeUserPasswordDTO.class)
            .check(Objects::nonNull, "Password payload cannot be null")
            .check(p -> p.currentPassword() != null && !p.currentPassword().isBlank(), "Current password cannot be empty")
            .check(p -> p.newPassword() != null && !p.newPassword().isBlank(), "New password cannot be empty")
            .get();

        UserDTO userDTO = userService.changePassword(authUser, targetUserId, dto);

        ctx.status(200).json(userDTO);
    }

    @Override
    public void getById(Context ctx)
    {
        Long id = RequestUtil.requirePathId(ctx, "id");

        UserDTO userDTO = userService.findById(id);
        ctx.status(200).json(userDTO);
    }

    @Override
    public void getAll(Context ctx)
    {
        List<UserDTO> users = userService.findAll();
        ctx.status(200).json(users);
    }

    @Override
    public void create(Context ctx)
    {
        CreateUserRequestDTO dto = ctx.bodyValidator(CreateUserRequestDTO.class)
            .check(Objects::nonNull, "Payload cant be null")
            .get();

        UserDTO userDTO = userService.registerUser(dto);
        ctx.status(201).json(userDTO);
    }

    @Override
    public void update(Context ctx)
    {
        AuthenticatedUser authUser = SecurityUtil.getAuthenticatedUser(ctx);
        Long userId = RequestUtil.requirePathId(ctx, "id");

        UpdateUserDTO dto = ctx.bodyValidator(UpdateUserDTO.class)
            .check(Objects::nonNull, "Update payload cannot be null")
            .get();

        UserDTO userDTO = userService.update(authUser, userId, dto);
        ctx.status(200).json(userDTO);
    }

    @Override
    public void delete(Context ctx)
    {
        AuthenticatedUser authUser = SecurityUtil.getAuthenticatedUser(ctx);
        Long targetUserId = RequestUtil.requirePathId(ctx, "id");

        boolean deleted = userService.delete(authUser, targetUserId);
        ctx.status(deleted ? 204 : 404);
    }

    @Override
    public void getMe(Context ctx)
    {
        AuthenticatedUser authUser = SecurityUtil.getAuthenticatedUser(ctx);
        UserDTO userDTO = userService.findById(authUser.userId());
        ctx.status(200).json(userDTO);
    }
}
