package app.services.interfaces;

import app.security.dtos.LoginRequestDTO;
import app.security.dtos.RegisterRequestDTO;
import app.security.dtos.UserSecurityDTO;

public interface ISecurityService
{
    UserSecurityDTO register(RegisterRequestDTO request);
    String login(LoginRequestDTO request);
}