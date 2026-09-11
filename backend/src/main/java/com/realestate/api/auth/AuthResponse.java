package com.realestate.api.auth;

public record AuthResponse(String token, UserView user) {}
