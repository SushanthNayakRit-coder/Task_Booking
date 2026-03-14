package com.taskbooking.service;

import com.taskbooking.dto.UserResponse;
import com.taskbooking.entity.User;
import com.taskbooking.entity.UserRole;
import com.taskbooking.repository.UserRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public UserService(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    public List<UserResponse> findAllUsers() {
        return userRepository.findAll().stream()
            .map(this::toResponse)
            .collect(Collectors.toList());
    }

    public UserResponse toResponse(User user) {
        UserResponse r = new UserResponse();
        r.setId(user.getId());
        r.setUsername(user.getUsername());
        r.setName(user.getName());
        r.setRole(user.getRole());
        return r;
    }

    @Transactional
    public void createDefaultUsersIfEmpty() {
        if (userRepository.count() > 0) return;

        userRepository.save(new User("admin", passwordEncoder.encode("admin"), "Admin User", UserRole.ADMIN));
        userRepository.save(new User("manager", passwordEncoder.encode("manager"), "Manager User", UserRole.MANAGER));
        userRepository.save(new User("user", passwordEncoder.encode("user"), "Regular User", UserRole.USER));
    }
}
