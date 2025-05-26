package com.dolgosheev.bankcardmanagement.config;

import com.dolgosheev.bankcardmanagement.security.JwtAuthEntryPoint;
import com.dolgosheev.bankcardmanagement.security.JwtAuthTokenFilter;
import com.dolgosheev.bankcardmanagement.security.UserDetailsServiceImpl;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.Arrays;

/**
 * Конфигурация безопасности веб-приложения.
 * Настраивает аутентификацию, авторизацию, CORS и JWT фильтры.
 * 
 * @author Dolgosheev
 * @version 1.0
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity
@RequiredArgsConstructor
public class WebSecurityConfig {

    private final UserDetailsServiceImpl userDetailsService;
    private final JwtAuthEntryPoint unauthorizedHandler;
    private final JwtAuthTokenFilter jwtAuthTokenFilter;

    /**
     * Настраивает провайдер аутентификации DAO.
     * 
     * @return настроенный DaoAuthenticationProvider
     */
    @Bean
    public DaoAuthenticationProvider authenticationProvider() {
        DaoAuthenticationProvider authProvider = new DaoAuthenticationProvider();
        
        authProvider.setUserDetailsService(userDetailsService);
        authProvider.setPasswordEncoder(passwordEncoder());
        
        return authProvider;
    }

    /**
     * Создает менеджер аутентификации.
     * 
     * @param authConfig конфигурация аутентификации
     * @return AuthenticationManager
     * @throws Exception если произошла ошибка при создании
     */
    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration authConfig) throws Exception {
        return authConfig.getAuthenticationManager();
    }

    /**
     * Создает кодировщик паролей BCrypt.
     * 
     * @return BCryptPasswordEncoder для хеширования паролей
     */
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    /**
     * Настраивает CORS (Cross-Origin Resource Sharing) для разрешения запросов
     * от фронтенд-приложений, работающих на других доменах/портах.
     * 
     * @return источник конфигурации CORS
     */
    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        
        // Разрешаем запросы с любых доменов (в продакшене следует указать конкретные домены)
        configuration.setAllowedOriginPatterns(Arrays.asList("*"));
        
        // Разрешаем основные HTTP методы
        configuration.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "DELETE", "OPTIONS", "PATCH"));
        
        // Разрешаем основные заголовки
        configuration.setAllowedHeaders(Arrays.asList("*"));
        
        // Разрешаем отправку cookies и авторизационных заголовков
        configuration.setAllowCredentials(true);
        
        // Указываем, какие заголовки могут быть доступны клиенту
        configuration.setExposedHeaders(Arrays.asList("Authorization"));
        
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        
        return source;
    }

    /**
     * Настраивает цепочку фильтров безопасности.
     * Конфигурирует CORS, CSRF, аутентификацию и авторизацию.
     * 
     * @param http объект конфигурации HTTP безопасности
     * @return настроенная цепочка фильтров безопасности
     * @throws Exception если произошла ошибка при конфигурации
     */
    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                // Используем нашу кастомную CORS конфигурацию
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))
                // Отключаем CSRF для REST API
                .csrf(AbstractHttpConfigurer::disable)
                // Настраиваем обработку исключений аутентификации
                .exceptionHandling(exception -> exception.authenticationEntryPoint(unauthorizedHandler))
                // Используем stateless сессии для JWT
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                // Настраиваем правила авторизации
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/").permitAll() // Главная страница
                        .requestMatchers("/api/auth/**").permitAll() // Эндпоинты аутентификации
                        .requestMatchers("/debug/**").permitAll() // Отладочные эндпоинты
                        .requestMatchers("/api-docs/**", "/swagger-ui/**", "/swagger-ui.html", "/openapi.yaml").permitAll() // Swagger
                        .anyRequest().authenticated() // Все остальные запросы требуют аутентификации
                );

        // Добавляем провайдер аутентификации
        http.authenticationProvider(authenticationProvider());
        // Добавляем JWT фильтр перед стандартным фильтром аутентификации
        http.addFilterBefore(jwtAuthTokenFilter, UsernamePasswordAuthenticationFilter.class);
        
        return http.build();
    }
} 