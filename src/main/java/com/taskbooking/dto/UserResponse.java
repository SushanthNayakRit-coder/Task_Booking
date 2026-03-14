package com.taskbooking.dto;

import com.taskbooking.entity.UserRole;

public class UserResponse {
    private Long id;
    private String username;
    private String name;
    private UserRole role;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public UserRole getRole() { return role; }
    public void setRole(UserRole role) { this.role = role; }
}
