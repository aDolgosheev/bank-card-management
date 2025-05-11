package com.dolgosheev.bankcardmanagement.controller;

import com.dolgosheev.bankcardmanagement.entity.User;
import com.dolgosheev.bankcardmanagement.repository.UserRepository;
import com.dolgosheev.bankcardmanagement.security.UserDetailsImpl;
import com.dolgosheev.bankcardmanagement.security.UserDetailsServiceImpl;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Optional;

@RestController
@RequestMapping("/debug")
@RequiredArgsConstructor
public class DebugController {

    private final UserRepository userRepository;
    private final UserDetailsServiceImpl userDetailsService;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;

    @GetMapping("/check-user")
    public String checkUser(@RequestParam String email) {
        Optional<User> userOpt = userRepository.findByEmail(email);
        if (userOpt.isPresent()) {
            User user = userOpt.get();
            return "Пользователь найден: " + user.getEmail() + 
                   "\nID: " + user.getId() + 
                   "\nИмя: " + user.getFirstName() + 
                   "\nФамилия: " + user.getLastName() + 
                   "\nРоли: " + user.getRoles();
        } else {
            return "Пользователь с email " + email + " не найден";
        }
    }

    @GetMapping("/check-password")
    public String checkPassword(@RequestParam String email, @RequestParam String password) {
        Optional<User> userOpt = userRepository.findByEmail(email);
        if (userOpt.isPresent()) {
            User user = userOpt.get();
            boolean matches = passwordEncoder.matches(password, user.getPassword());
            return "Проверка пароля для пользователя " + email + ": " + 
                   (matches ? "УСПЕШНО" : "НЕУСПЕШНО") +
                   "\nХранимый хеш: " + user.getPassword();
        } else {
            return "Пользователь с email " + email + " не найден";
        }
    }

    @GetMapping("/test-auth")
    public String testAuth(@RequestParam String email, @RequestParam String password) {
        try {
            Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(email, password)
            );
            UserDetailsImpl userDetails = (UserDetailsImpl) authentication.getPrincipal();
            return "Аутентификация успешна!\n" +
                   "Пользователь: " + userDetails.getUsername() + "\n" +
                   "Роли: " + userDetails.getAuthorities();
        } catch (Exception e) {
            return "Ошибка аутентификации: " + e.getMessage();
        }
    }

    @GetMapping("/generate-password")
    public String generatePassword(@RequestParam String password) {
        String encoded = passwordEncoder.encode(password);
        return "Закодированный пароль для '" + password + "': " + encoded;
    }

    @GetMapping("/update-password")
    public String updatePassword(@RequestParam String email, @RequestParam String password) {
        Optional<User> userOpt = userRepository.findByEmail(email);
        if (userOpt.isPresent()) {
            User user = userOpt.get();
            String encoded = passwordEncoder.encode(password);
            user.setPassword(encoded);
            userRepository.save(user);
            return "Пароль для пользователя " + email + " обновлен на '" + password + "'\nНовый хеш: " + encoded;
        } else {
            return "Пользователь с email " + email + " не найден";
        }
    }
} 