package com.realestate.api.security;

/**
 * The logged-in caller, decoded from the JWT by JwtAuthFilter. Controllers
 * receive this via @AuthenticationPrincipal instead of re-querying the
 * database for basic identity info on every request.
 */
public record AuthenticatedUser(String id, String email, String name, String role) {}
