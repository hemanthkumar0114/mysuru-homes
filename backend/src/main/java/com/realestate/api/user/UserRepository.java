package com.realestate.api.user;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserRepository extends JpaRepository<User, String> {
    Optional<User> findByEmail(String email);

    boolean existsByEmail(String email);

    default User requireById(String id) {
        return findById(id).orElseThrow(() -> new IllegalStateException("Authenticated user vanished: " + id));
    }
}
