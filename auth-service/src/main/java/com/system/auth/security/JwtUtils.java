package com.system.auth.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import org.springframework.stereotype.Component;

import java.security.Key;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

//import static jdk.internal.org.jline.keymap.KeyMap.key;

@Component
public class JwtUtils {

    // THE MASTER SECRET KEY (Must be exactly this)
    public static final String SECRET = "5367566B59703373367639792F423F4528482B4D6251655468576D5A71347437";

    public String generateToken(String userName, String role) {
        Map<String, Object> claims = new HashMap<>();
        claims.put("role", role);
        return Jwts.builder()
                .setClaims(claims)
                .setSubject(userName)
                .setIssuedAt(new Date(System.currentTimeMillis()))
                .setExpiration(new Date(System.currentTimeMillis() + 1000 * 60 * 60 * 24)) // 24 hours
                .signWith(getSignKey(), SignatureAlgorithm.HS256)
                .compact();
    }

    public Claims validateTokenAndGetClaims(String token) {
        return Jwts.parserBuilder()
                .setSigningKey(getSignKey())
                .build()
                .parseClaimsJws(token)
                .getBody();
    }

    private Key getSignKey() {
        byte[] keyBytes = Decoders.BASE64.decode(SECRET);
        return Keys.hmacShaKeyFor(keyBytes);
    }

    public Date extractExpiration(String token) {
        return Jwts.parserBuilder()
                .setSigningKey(getSignKey()) // Use whatever key method you currently have
                .build()
                .parseClaimsJws(token)
                .getBody()
                .getExpiration();
    }
}