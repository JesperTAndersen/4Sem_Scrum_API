package app.utils;

import org.mindrot.jbcrypt.BCrypt;

public class PasswordUtil
{
    private PasswordUtil(){}

    public static String hashPassword(String plainPassword, int salt)
    {
        if (plainPassword == null || plainPassword.isEmpty())
        {
            throw new IllegalArgumentException("Password cannot be empty or null");
        }
        return BCrypt.hashpw(plainPassword, BCrypt.gensalt(salt));
    }

    public static boolean verifyPassword(String plainPassword, String hashedPassword)
    {
        if (plainPassword == null || plainPassword.isBlank())
        {
            return false;
        }
        return BCrypt.checkpw(plainPassword, hashedPassword);
    }
}
