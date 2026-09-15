package app.utils;

import org.mindrot.jbcrypt.BCrypt;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;

public final class PasswordUtil
{
    private static final int BCRYPT_COST = 12;
    private static final String SHA_ALGORITHM = "SHA-256";

    private PasswordUtil()
    {
    }

    public static String hashPassword(String plainPassword)
    {
        requireValid(plainPassword, "Password");
        String prehashedPassword = prehash(plainPassword);
        return BCrypt.hashpw(prehashedPassword, BCrypt.gensalt(BCRYPT_COST));
    }

    public static String hashPassword(String plainPassword, int cost)
    {
        requireValid(plainPassword, "Password");
        String prehashedPassword = prehash(plainPassword);
        return BCrypt.hashpw(prehashedPassword, BCrypt.gensalt(cost));
    }

    public static boolean verifyPassword(String plainPassword, String hashedPassword)
    {
        if (isNullOrBlank(plainPassword) || isNullOrBlank(hashedPassword))
        {
            return false;
        }
        String preHashedPassword = prehash(plainPassword);
        return BCrypt.checkpw(preHashedPassword, hashedPassword);
    }

    public static boolean needsRehash(String hashedPassword)
    {
        if (isNullOrBlank(hashedPassword))
        {
            return true;
        }
        String[] parts = hashedPassword.split("\\$");
        int cost = Integer.parseInt(parts[2]);
        return cost < BCRYPT_COST;
    }

    private static String prehash(String plainPassword)
    {
        try
        {
            MessageDigest digest = MessageDigest.getInstance(SHA_ALGORITHM);
            byte[] hash = digest.digest(plainPassword.getBytes(StandardCharsets.UTF_8));
            return Base64.getEncoder().encodeToString(hash);
        }
        catch (NoSuchAlgorithmException e)
        {
            throw new RuntimeException("Prehashing failed due to environment configuration: " + e.getMessage(), e);
        }
    }

    private static void requireValid(String value, String label)
    {
        if (isNullOrBlank(value))
        {
            throw new IllegalArgumentException(label + " cannot be null or blank");
        }
    }

    private static boolean isNullOrBlank(String value)
    {
        return value == null || value.isBlank();
    }
}
