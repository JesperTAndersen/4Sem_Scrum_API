package app.security.domain;

import app.security.presentation.dto.AuthenticatedUser;
import app.security.presentation.dto.LoginRequestDTO;
import app.user.presentation.dto.CreateUserRequestDTO;

public interface ISecurityService
{
    AuthenticatedUser register(CreateUserRequestDTO request);
    String login(LoginRequestDTO request);
}