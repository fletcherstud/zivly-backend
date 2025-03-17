package com.zivly.edge.security;

import com.zivly.edge.model.entity.User;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.Date;

@Component
public class JwtUtil {

    @Value("${jwt.access-secret}")
    private String accessSecret;

    @Value("${jwt.refresh-secret}")
    private String refreshSecret;

    @Value("${jwt.access-expiration}")
    private long accessExpiration;

    @Value("${jwt.refresh-expiration}")
    private long refreshExpiration;

    public String generateAccessToken(User user) {
        return Jwts.builder()
                .setSubject(user.getEmail())
                .setIssuedAt(new Date())
                .setExpiration(new Date(System.currentTimeMillis() + accessExpiration * 1000))
                .signWith(SignatureAlgorithm.HS512, accessSecret)
                .compact();
    }

    public String generateRefreshToken(User user) {
        return Jwts.builder()
                .setSubject(user.getEmail())
                .setIssuedAt(new Date())
                .setExpiration(new Date(System.currentTimeMillis() + refreshExpiration * 1000))
                .signWith(SignatureAlgorithm.HS512, refreshSecret)
                .compact();
    }

    public String getEmailFromToken(String token, boolean isRefreshToken) {
        String secret = isRefreshToken ? refreshSecret : accessSecret;
        return Jwts.parser().setSigningKey(secret).build().parseClaimsJws(token).getBody().getSubject();
    }

    public boolean validateToken(String token, boolean isRefreshToken) {
        try {
            String secret = isRefreshToken ? refreshSecret : accessSecret;
            Jwts.parser().setSigningKey(secret).build().parseClaimsJws(token);
            return true;
        } catch (Exception e) {
            return false;
        }
    }
}