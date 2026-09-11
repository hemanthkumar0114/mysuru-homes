package com.realestate.api.auth;

import com.realestate.api.user.User;
import com.realestate.api.user.UserRole;

/** What the frontend is allowed to know about a user - never the password hash. */
public record UserView(String id, String name, String email, UserRole role) {
    public static UserView from(User user) {
        return new UserView(user.getId(), user.getName(), user.getEmail(), user.getRole());
    }
}
