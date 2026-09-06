package com.harsh.collab_board.security;


import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;


import java.nio.charset.StandardCharsets;
import javax.crypto.SecretKey;
import java.util.Date;

@Component
public class JwtUtil {

    private final SecretKey secretKey;
        private final long EXPIRATION_TIME = 1000 * 60 * 60 * 24 ;

    public JwtUtil(@Value("${jwt.secret}") String secret) {
                    this.secretKey = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
                }

        public String generateToken(String username ) {
            return Jwts.builder()
                    .subject(username)
                    .issuedAt(new Date())
                    .expiration(new Date(System.currentTimeMillis() + EXPIRATION_TIME) )
                    .signWith(secretKey)
                    .compact() ;
        }

        public String extractUsername(String token) {
            return Jwts.parser()
                    .verifyWith(secretKey)
                    .build()
                    .parseSignedClaims(token)
                    .getPayload()
                    .getSubject() ;
        }

        public boolean isTokenValid(String token , String username ) {
            try {
                String extractedUsername = extractUsername(token) ;
                return extractedUsername.equals(username) && !isTokenExpired(token) ;
            } catch (Exception e) {
                return false ;
            }
        }

        private boolean isTokenExpired(String token ) {
            Date expiration = Jwts.parser()
                    .verifyWith(secretKey)
                    .build()
                    .parseSignedClaims(token)
                    .getPayload()
                    .getExpiration() ;
            return expiration.before(new Date()) ;
        }
}
