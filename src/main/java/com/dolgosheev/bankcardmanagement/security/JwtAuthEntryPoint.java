package com.dolgosheev.bankcardmanagement.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

/**
 * Точка входа для обработки ошибок аутентификации JWT.
 * Возвращает JSON ответ с информацией об ошибке аутентификации.
 * 
 * @author Dolgosheev
 * @version 1.0
 */
@Component
@RequiredArgsConstructor
public class JwtAuthEntryPoint implements AuthenticationEntryPoint {
    
    private final ObjectMapper objectMapper;

    /**
     * Обрабатывает ошибки аутентификации и возвращает JSON ответ с деталями ошибки.
     * 
     * @param request HTTP запрос
     * @param response HTTP ответ
     * @param authException исключение аутентификации
     * @throws IOException если произошла ошибка при записи ответа
     */
    @Override
    public void commence(HttpServletRequest request, HttpServletResponse response, AuthenticationException authException)
            throws IOException {
        
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);

        Map<String, Object> body = new HashMap<>();
        body.put("status", HttpServletResponse.SC_UNAUTHORIZED);
        body.put("error", "Unauthorized");
        body.put("message", authException.getMessage());
        body.put("path", request.getServletPath());

        objectMapper.writeValue(response.getOutputStream(), body);
    }
} 