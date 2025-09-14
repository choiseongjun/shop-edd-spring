package com.jun.userservice.controller;

import com.jun.userservice.dto.UserDto;
import com.jun.userservice.entity.User;
import com.jun.userservice.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/users")
@CrossOrigin(origins = "*", maxAge = 3600)
public class UserController {
    
    @Autowired
    private UserService userService;
    
    @GetMapping("/profile")
    public ResponseEntity<UserDto> getCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        User user = (User) authentication.getPrincipal();
        UserDto userDto = userService.getUserById(user.getId());
        return ResponseEntity.ok(userDto);
    }
    
    @PutMapping("/profile")
    public ResponseEntity<?> updateCurrentUser(@RequestBody UserDto userDto) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        User user = (User) authentication.getPrincipal();
        
        UserDto updatedUser = userService.updateUser(user.getId(), userDto);
        if (updatedUser != null) {
            Map<String, Object> response = new HashMap<>();
            response.put("message", "Profile updated successfully");
            response.put("user", updatedUser);
            return ResponseEntity.ok(response);
        }
        
        Map<String, String> errorResponse = new HashMap<>();
        errorResponse.put("error", "Failed to update profile");
        return ResponseEntity.badRequest().body(errorResponse);
    }
    
    @PostMapping("/change-password")
    public ResponseEntity<?> changePassword(
            @RequestParam String oldPassword,
            @RequestParam String newPassword) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        User user = (User) authentication.getPrincipal();
        
        boolean success = userService.changePassword(user.getId(), oldPassword, newPassword);
        Map<String, Object> response = new HashMap<>();
        
        if (success) {
            response.put("message", "Password changed successfully");
            return ResponseEntity.ok(response);
        } else {
            response.put("error", "Failed to change password - invalid old password");
            return ResponseEntity.badRequest().body(response);
        }
    }
    
    @GetMapping("/admin/all")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<UserDto>> getAllUsers() {
        List<UserDto> users = userService.getAllUsers();
        return ResponseEntity.ok(users);
    }
    
    @GetMapping("/admin/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<UserDto> getUserById(@PathVariable Long id) {
        UserDto user = userService.getUserById(id);
        if (user != null) {
            return ResponseEntity.ok(user);
        }
        return ResponseEntity.notFound().build();
    }
    
    @PutMapping("/admin/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> updateUserByAdmin(@PathVariable Long id, @RequestBody UserDto userDto) {
        UserDto updatedUser = userService.updateUser(id, userDto);
        if (updatedUser != null) {
            Map<String, Object> response = new HashMap<>();
            response.put("message", "User updated successfully");
            response.put("user", updatedUser);
            return ResponseEntity.ok(response);
        }
        
        Map<String, String> errorResponse = new HashMap<>();
        errorResponse.put("error", "User not found");
        return ResponseEntity.notFound().build();
    }
    
    @PostMapping("/admin/{id}/activate")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> activateUser(@PathVariable Long id) {
        userService.updateActiveStatus(id, true);
        Map<String, String> response = new HashMap<>();
        response.put("message", "User activated successfully");
        return ResponseEntity.ok(response);
    }
    
    @PostMapping("/admin/{id}/deactivate")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> deactivateUser(@PathVariable Long id) {
        userService.updateActiveStatus(id, false);
        Map<String, String> response = new HashMap<>();
        response.put("message", "User deactivated successfully");
        return ResponseEntity.ok(response);
    }
    
    @PostMapping("/admin/{id}/verify-email")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> verifyUserEmail(@PathVariable Long id) {
        userService.verifyEmail(id);
        Map<String, String> response = new HashMap<>();
        response.put("message", "User email verified successfully");
        return ResponseEntity.ok(response);
    }
    
    @GetMapping("/health")
    public ResponseEntity<Map<String, String>> health() {
        Map<String, String> status = new HashMap<>();
        status.put("status", "UP");
        status.put("service", "user-service");
        status.put("timestamp", String.valueOf(System.currentTimeMillis()));
        return ResponseEntity.ok(status);
    }
}