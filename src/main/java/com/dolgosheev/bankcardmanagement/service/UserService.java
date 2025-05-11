package com.dolgosheev.bankcardmanagement.service;

import com.dolgosheev.bankcardmanagement.entity.Role;
import com.dolgosheev.bankcardmanagement.entity.User;
import com.dolgosheev.bankcardmanagement.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class UserService {
    
    private final UserRepository userRepository;

    @Transactional(readOnly = true)
    public Page<User> getAllUsers(Pageable pageable) {
        return userRepository.findAll(pageable);
    }

    @Transactional(readOnly = true)
    public User getUserById(Long id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new UsernameNotFoundException("User with id " + id + " not found"));
    }

    @Transactional(readOnly = true)
    public User getUserByEmail(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new UsernameNotFoundException("User with email " + email + " not found"));
    }

    @Transactional
    public void deleteUser(Long id) {
        User user = getUserById(id);
        userRepository.delete(user);
    }

    @Transactional(readOnly = true)
    public User getCurrentUser(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new UsernameNotFoundException("Current user not found"));
    }

    @Transactional(readOnly = true)
    public void validateUserAccess(String userEmail, Long userId) {
        User currentUser = getCurrentUser(userEmail);
        
        // Check if user has access to the requested data
        if (!currentUser.getId().equals(userId) && 
                !currentUser.getRoles().stream().anyMatch(r -> r.getName() == Role.ERole.ROLE_ADMIN)) {
            throw new AccessDeniedException("No access to user data with id " + userId);
        }
    }
} 