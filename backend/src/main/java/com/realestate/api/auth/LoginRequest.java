package com.realestate.api.auth;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record LoginRequest(
        @NotBlank @Size(max = 254, message = "Email must be 254 characters or fewer") String email,
        @NotBlank @Size(max = 128, message = "Password is too long") String password) {}
