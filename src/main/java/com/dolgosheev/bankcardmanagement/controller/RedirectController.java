package com.dolgosheev.bankcardmanagement.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

/**
 * Контроллер для перенаправления с главной страницы на Swagger UI.
 * 
 * @author Dolgosheev
 * @version 1.0
 */
@Controller
public class RedirectController {

    /**
     * Перенаправляет пользователя с корневого URL на страницу Swagger UI.
     * 
     * @return строка перенаправления на Swagger UI
     */
    @GetMapping("/")
    public String redirectToSwaggerUi() {
        return "redirect:/swagger-ui/index.html";
    }
} 