package com.dolgosheev.bankcardmanagement.security;

import java.nio.charset.StandardCharsets;
import java.util.Date;

import javax.crypto.SecretKey;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;

import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.MalformedJwtException;
import io.jsonwebtoken.UnsupportedJwtException;
import io.jsonwebtoken.security.Keys;
import io.jsonwebtoken.security.SignatureException;
import lombok.extern.slf4j.Slf4j;

/**
 * Утилитный класс для работы с JWT токенами.
 * Предоставляет методы для генерации, валидации и извлечения данных из JWT токенов.
 * 
 * @author Dolgosheev
 * @version 1.0
 */
@Component
@Slf4j
public class JwtUtils {

    /** Секретный ключ для подписи JWT токенов */
    @Value("${jwt.secret}")
    private String jwtSecret;

    /** Время жизни JWT токена в миллисекундах */
    @Value("${jwt.expiration}")
    private int jwtExpirationMs;

    /**
     * Генерирует JWT токен на основе данных аутентификации.
     * 
     * @param authentication объект аутентификации Spring Security
     * @return строка JWT токена
     * @throws IllegalArgumentException если authentication равен null
     */
    public String generateJwtToken(Authentication authentication) {
        UserDetailsImpl userPrincipal = (UserDetailsImpl) authentication.getPrincipal();

        // Создаем секретный ключ из строки конфигурации
        SecretKey key = Keys.hmacShaKeyFor(jwtSecret.getBytes(StandardCharsets.UTF_8));

        // Строим JWT токен с использованием современного API
        return Jwts.builder()
                .subject(userPrincipal.getUsername()) // Современный API вместо setSubject()
                .issuedAt(new Date()) // Современный API вместо setIssuedAt()
                .expiration(new Date((new Date()).getTime() + jwtExpirationMs)) // Современный API вместо setExpiration()
                .signWith(key, Jwts.SIG.HS512) // Подписываем токен современным API
                .compact();
    }

    /**
     * Извлекает имя пользователя из JWT токена.
     * 
     * @param token JWT токен в виде строки
     * @return имя пользователя из токена
     * @throws io.jsonwebtoken.JwtException если токен невалиден
     */
    public String getUserNameFromJwtToken(String token) {
        SecretKey key = Keys.hmacShaKeyFor(jwtSecret.getBytes(StandardCharsets.UTF_8));

        // Используем современный API verifyWith() вместо устаревшего setSigningKey()
        return Jwts.parser()
                .verifyWith(key) // Новый метод вместо setSigningKey()
                .build()
                .parseClaimsJws(token)
                .getPayload() // Новый метод вместо getBody()
                .getSubject();
    }

    /**
     * Валидирует JWT токен.
     * 
     * @param authToken JWT токен для валидации
     * @return true если токен валиден, false в противном случае
     */
    public boolean validateJwtToken(String authToken) {
        try {
            SecretKey key = Keys.hmacShaKeyFor(jwtSecret.getBytes(StandardCharsets.UTF_8));

            // Используем современный API verifyWith() для валидации токена
            Jwts.parser()
                    .verifyWith(key) // Новый метод вместо setSigningKey()
                    .build()
                    .parseClaimsJws(authToken);
            return true;
        } catch (SignatureException e) {
            log.error("Invalid JWT signature: {}", e.getMessage());
        } catch (MalformedJwtException e) {
            log.error("Invalid JWT token: {}", e.getMessage());
        } catch (ExpiredJwtException e) {
            log.error("JWT token is expired: {}", e.getMessage());
        } catch (UnsupportedJwtException e) {
            log.error("JWT token is unsupported: {}", e.getMessage());
        } catch (IllegalArgumentException e) {
            log.error("JWT claims string is empty: {}", e.getMessage());
        }

        return false;
    }
}