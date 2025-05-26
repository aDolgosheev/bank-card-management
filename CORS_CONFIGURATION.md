# Конфигурация CORS

## Текущая настройка

В данный момент CORS настроен для разработки с разрешением запросов с любых доменов (`*`). 

## Настройка для продакшена

⚠️ **ВАЖНО**: Перед развертыванием в продакшене необходимо изменить CORS конфигурацию!

### Что нужно изменить в `WebSecurityConfig.java`:

```java
// Вместо этого (небезопасно для продакшена):
configuration.setAllowedOriginPatterns(Arrays.asList("*"));

// Используйте конкретные домены:
configuration.setAllowedOrigins(Arrays.asList(
    "https://yourdomain.com",
    "https://www.yourdomain.com",
    "https://app.yourdomain.com"
));
```

### Рекомендуемая конфигурация для продакшена:

```java
@Bean
public CorsConfigurationSource corsConfigurationSource() {
    CorsConfiguration configuration = new CorsConfiguration();
    
    // Указываем конкретные домены фронтенда
    configuration.setAllowedOrigins(Arrays.asList(
        "https://yourdomain.com",
        "https://www.yourdomain.com"
    ));
    
    // Разрешаем только необходимые методы
    configuration.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "DELETE"));
    
    // Разрешаем только необходимые заголовки
    configuration.setAllowedHeaders(Arrays.asList(
        "Authorization", 
        "Content-Type", 
        "X-Requested-With"
    ));
    
    configuration.setAllowCredentials(true);
    configuration.setExposedHeaders(Arrays.asList("Authorization"));
    
    UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
    source.registerCorsConfiguration("/api/**", configuration);
    
    return source;
}
```

### Переменные окружения

Можно также вынести CORS настройки в переменные окружения:

```properties
# application.properties
cors.allowed.origins=https://yourdomain.com,https://www.yourdomain.com
cors.allowed.methods=GET,POST,PUT,DELETE
cors.allowed.headers=Authorization,Content-Type,X-Requested-With
```

И использовать их в конфигурации:

```java
@Value("${cors.allowed.origins}")
private String[] allowedOrigins;

@Value("${cors.allowed.methods}")
private String[] allowedMethods;

@Value("${cors.allowed.headers}")
private String[] allowedHeaders;
``` 