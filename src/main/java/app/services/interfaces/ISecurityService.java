package app.services.interfaces;

import app.dtos.security.AuthenticatedUser;
import app.dtos.security.LoginRequestDTO;
import app.dtos.user.CreateUserRequestDTO;

public interface ISecurityService
{
    AuthenticatedUser register(CreateUserRequestDTO request);
    String login(LoginRequestDTO request);
}