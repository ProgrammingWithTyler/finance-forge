package com.financeforge.api.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

/**
 * Service for JWT token generation, validation, and parsing.
 * <p>
 * This service handles all JWT-related operations:
 * <ul>
 *   <li>Generating access tokens (short-lived, 15 minutes)</li>
 *   <li>Generating refresh tokens (long-lived, 7 days)</li>
 *   <li>Validating tokens (signature + expiration)</li>
 *   <li>Extracting claims (username, expiration, custom claims)</li>
 * </ul>
 * <p>
 * <b>How JWT Works:</b>
 * <pre>
 * 1. User logs in successfully
 * 2. Server generates JWT with user info (username, expiration)
 * 3. JWT is signed with secret key (prevents tampering)
 * 4. Client stores JWT and sends it with every request
 * 5. Server validates JWT signature and checks expiration
 * 6. If valid, user is authenticated
 * </pre>
 * <p>
 * <b>JWT Structure:</b> Header.Payload.Signature
 * <ul>
 *   <li>Header: Algorithm (HS256) + Token Type (JWT)</li>
 *   <li>Payload: Claims (username, expiration, custom data)</li>
 *   <li>Signature: HMACSHA256(base64(header) + base64(payload), secret)</li>
 * </ul>
 *
 * @author ProgrammingWithTyler
 * @since 1.0
 */
@Service
public class JwtService {

    // Inject from application.yml
    @Value("${jwt.secret}")
    private String secret;

    @Value("${jwt.expiration}")
    private Long accessTokenExpiration; // 15 minutes in milliseconds

    @Value("${jwt.refresh-expiration}")
    private Long refreshTokenExpiration; // 7 days in milliseconds

    /**
     * Converts the secret string to a cryptographic key for signing JWTs.
     * <p>
     * JJWT requires a SecretKey object, not just a string.
     * We use HMAC-SHA256 algorithm which requires a 256-bit key.
     *
     * @return the signing key derived from the secret
     */
    private SecretKey getSigningKey() {
        return Keys.hmacShaKeyFor(secret.getBytes());
    }

    /**
     * Generates an access token for the authenticated user.
     * <p>
     * Access tokens are short-lived (15 minutes) and used for API requests.
     * They contain the username and a "type" claim to identify them as access tokens.
     *
     * @param userDetails the authenticated user's details
     * @return a signed JWT access token
     */
    public String generateAccessToken(UserDetails userDetails) {
        Map<String, Object> claims = new HashMap<>();
        claims.put("type", "access");
        return generateToken(claims, userDetails.getUsername(), accessTokenExpiration);
    }

    /**
     * Generates a refresh token for the authenticated user.
     * <p>
     * Refresh tokens are long-lived (7 days) and used to obtain new access tokens
     * without requiring the user to log in again.
     * They contain the username and a "type" claim to identify them as refresh tokens.
     *
     * @param userDetails the authenticated user's details
     * @return a signed JWT refresh token
     */
    public String generateRefreshToken(UserDetails userDetails) {
        Map<String, Object> claims = new HashMap<>();
        claims.put("type", "refresh");
        return generateToken(claims, userDetails.getUsername(), refreshTokenExpiration);
    }

    /**
     * Internal method to generate a JWT token with custom claims.
     * <p>
     * This is the core token generation logic used by both access and refresh token methods.
     * <p>
     * <b>Token Structure:</b>
     * <pre>
     * {
     *   "type": "access" or "refresh",  // Custom claim
     *   "sub": "username",               // Subject (standard claim)
     *   "iat": 1234567890,               // Issued at (standard claim)
     *   "exp": 1234567890                // Expiration (standard claim)
     * }
     * </pre>
     *
     * @param extraClaims additional claims to include in the token
     * @param username the subject (username) of the token
     * @param expiration expiration time in milliseconds from now
     * @return a signed JWT token string
     */
    private String generateToken(Map<String, Object> extraClaims, String username, Long expiration) {
        return Jwts.builder()
            .claims(extraClaims)                                             // Modern method (no "set" prefix)
            .subject(username)                                               // Modern method (no "set" prefix)
            .issuedAt(new Date(System.currentTimeMillis()))                 // Modern method (no "set" prefix)
            .expiration(new Date(System.currentTimeMillis() + expiration))  // Modern method (no "set" prefix)
            .signWith(getSigningKey())                                       // No need for algorithm parameter
            .compact();
    }

    /**
     * Extracts the username from a JWT token.
     * <p>
     * The username is stored in the "sub" (subject) claim, which is a standard JWT claim.
     *
     * @param token the JWT token string
     * @return the username extracted from the token
     * @throws io.jsonwebtoken.JwtException if the token is invalid or expired
     */
    public String extractUsername(String token) {
        return extractClaim(token, Claims::getSubject);
    }

    /**
     * Extracts the expiration date from a JWT token.
     * <p>
     * The expiration is stored in the "exp" (expiration) claim.
     *
     * @param token the JWT token string
     * @return the expiration date
     * @throws io.jsonwebtoken.JwtException if the token is invalid
     */
    public Date extractExpiration(String token) {
        return extractClaim(token, Claims::getExpiration);
    }

    /**
     * Extracts a specific claim from a JWT token using a claim resolver function.
     * <p>
     * This is a generic method that can extract any claim from the token.
     * It first parses the token to get all claims, then applies the resolver function.
     * <p>
     * <b>Example usage:</b>
     * <pre>
     * String username = extractClaim(token, Claims::getSubject);
     * Date expiration = extractClaim(token, Claims::getExpiration);
     * String type = extractClaim(token, claims -> claims.get("type", String.class));
     * </pre>
     *
     * @param token the JWT token string
     * @param claimsResolver function to extract the desired claim
     * @param <T> the type of the claim value
     * @return the extracted claim value
     * @throws io.jsonwebtoken.JwtException if the token is invalid
     */
    public <T> T extractClaim(String token, Function<Claims, T> claimsResolver) {
        final Claims claims = extractAllClaims(token);
        return claimsResolver.apply(claims);
    }

    /**
     * Extracts all claims from a JWT token.
     * <p>
     * This method parses and validates the JWT:
     * <ol>
     *   <li>Verifies the signature using our secret key</li>
     *   <li>Checks if the token is expired</li>
     *   <li>Returns all claims if valid</li>
     * </ol>
     * <p>
     * <b>Important:</b> This method throws exceptions if:
     * <ul>
     *   <li>Token signature is invalid (tampered token)</li>
     *   <li>Token is expired</li>
     *   <li>Token format is malformed</li>
     * </ul>
     *
     * @param token the JWT token string
     * @return all claims from the token
     * @throws io.jsonwebtoken.security.SignatureException if signature verification fails
     * @throws io.jsonwebtoken.ExpiredJwtException if token is expired
     * @throws io.jsonwebtoken.MalformedJwtException if token format is invalid
     */
    // This should already be fixed in the artifact:
    private Claims extractAllClaims(String token) {
        return Jwts.parser()
            .verifyWith(getSigningKey())
            .build()
            .parseSignedClaims(token)
            .getPayload();
    }

    /**
     * Checks if a JWT token is expired.
     * <p>
     * Compares the token's expiration date with the current date.
     *
     * @param token the JWT token string
     * @return true if the token is expired, false otherwise
     */
    public boolean isTokenExpired(String token) {
        return extractExpiration(token).before(new Date());
    }

    /**
     * Validates a JWT token against user details.
     * <p>
     * A token is valid if:
     * <ol>
     *   <li>The username in the token matches the provided user</li>
     *   <li>The token is not expired</li>
     *   <li>The signature is valid (checked during extraction)</li>
     * </ol>
     * <p>
     * This is the main validation method used by the authentication filter.
     *
     * @param token the JWT token string
     * @param userDetails the user details to validate against
     * @return true if the token is valid for this user, false otherwise
     */
    public boolean isTokenValid(String token, UserDetails userDetails) {
        final String username = extractUsername(token);
        return (username.equals(userDetails.getUsername()) && !isTokenExpired(token));
    }

    /**
     * Checks if a token is a refresh token.
     * <p>
     * We use a custom "type" claim to distinguish between access and refresh tokens.
     * This prevents someone from using a refresh token as an access token.
     *
     * @param token the JWT token string
     * @return true if it's a refresh token, false otherwise
     */
    public boolean isRefreshToken(String token) {
        Claims claims = extractAllClaims(token);
        return "refresh".equals(claims.get("type"));
    }

    /**
     * Gets the expiration time for access tokens in seconds.
     * <p>
     * This is used in the AuthResponse to tell the client how long
     * the access token is valid.
     *
     * @return expiration time in seconds (e.g., 900 for 15 minutes)
     */
    public Long getAccessTokenExpirationInSeconds() {
        return accessTokenExpiration / 1000;
    }
}