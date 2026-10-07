package com.realestate.api.auth;

import com.realestate.api.user.UserRole;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record RegisterRequest(
        @NotBlank @Size(max = 100, message = "Name must be 100 characters or fewer") String name,
        @NotBlank @Email @Size(max = 254, message = "Email must be 254 characters or fewer") String email,
        @NotBlank @Size(min = 8, max = 72, message = "Password must be between 8 and 72 characters") String password,
        @NotNull UserRole role) {}
