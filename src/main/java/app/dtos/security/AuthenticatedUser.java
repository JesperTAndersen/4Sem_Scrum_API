package app.dtos.security;

import app.enums.UserRole;

public record AuthenticatedUser(
    Long userId,
    String email,
    UserRole userRole
)
{
    public boolean isKitchenStaff()
    {
        return userRole == UserRole.HEAD_CHEF || userRole == UserRole.SOUS_CHEF || userRole == UserRole.LINE_COOK;
    }

    public boolean isHeadChef() {return userRole == UserRole.HEAD_CHEF;}

    public boolean isSousChef() {return userRole == UserRole.SOUS_CHEF;}

    public boolean isLineCook(){return userRole == UserRole.LINE_COOK;}
}
