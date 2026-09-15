package app.utils;

import app.exceptions.TokenCreationException;
import app.exceptions.TokenVerificationException;
import app.dtos.security.UserSecurityDTO;
import app.enums.Role;
import com.nimbusds.jose.*;
import com.nimbusds.jose.crypto.MACSigner;
import com.nimbusds.jose.crypto.MACVerifier;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;

import java.text.ParseException;
import java.util.Date;

public class JWTUtil
{

    private static final String ISSUER = System.getenv("JWT_ISSUER");
    private static final String SECRET = System.getenv("JWT_SECRET");
    private static final int EXPIRY = 1000 * 60 * 30; // 30 minutes
    private static final JWSSigner SIGNER;
    private static final JWSVerifier VERIFIER;

    static
    {
        if (SECRET == null)
        {
            throw new ExceptionInInitializerError("JWT_SECRET environment variable is not set");
        }
        if (SECRET.length() < 32)
        {
            throw new ExceptionInInitializerError("JWT_SECRET must be at least 32 characters");
        }
        try
        {
            SIGNER = new MACSigner(SECRET);
            VERIFIER = new MACVerifier(SECRET);
        }
        catch (JOSEException e)
        {
            throw new ExceptionInInitializerError(e);
        }
    }

    private JWTUtil()
    {
    }

    public static String createToken(Long id, String username, Role role) throws TokenCreationException
    {
        try
        {
            JWTClaimsSet claims = new JWTClaimsSet.Builder()
                    .subject(username)
                    .issuer(ISSUER)
                    .issueTime(new Date())
                    .claim("id", id)
                    .claim("role", role)
                    .expirationTime(new Date(System.currentTimeMillis() + EXPIRY))
                    .build();

            SignedJWT jwt = new SignedJWT(new JWSHeader(JWSAlgorithm.HS256), claims);
            jwt.sign(SIGNER);
            return jwt.serialize();
        }
        catch (JOSEException e)
        {
            throw new TokenCreationException("Could not create token", e);
        }
    }

    public static UserSecurityDTO parseToken(String token) // TODO Align DTO's
    {
        try
        {
            SignedJWT jwt = SignedJWT.parse(token);
            if (!jwt.verify(VERIFIER))
            {
                throw new TokenVerificationException("Invalid token signature");
            }

            JWTClaimsSet claims = jwt.getJWTClaimsSet();
            if (claims.getExpirationTime().before(new Date()))
            {
                throw new TokenVerificationException("Token expired");
            }

            return new UserSecurityDTO(
                    claims.getLongClaim("id"),
                    claims.getSubject(),
                    Role.valueOf(claims.getStringClaim("role"))
            );
        }
        catch (TokenVerificationException e)
        {
            throw e;
        }
        catch (ParseException | JOSEException e)
        {
            throw new TokenVerificationException("Invalid token", e);
        }
    }

    public static void validate()
    {
        if (SIGNER == null)
        {
            throw new ExceptionInInitializerError("JWTUtil failed to initialize");
        }
    }

}