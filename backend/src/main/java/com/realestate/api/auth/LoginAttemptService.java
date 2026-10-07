package com.realestate.api.auth;

import java.time.Duration;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class LoginAttemptService {

    private static final int CLEANUP_THRESHOLD = 10_000;

    private final int maxFailures;
    private final long lockoutMillis;
    private final Map<String, Failures> failuresByEmail = new ConcurrentHashMap<>();

    public LoginAttemptService(
            @Value("${app.auth.max-failed-logins:5}") int maxFailures,
            @Value("${app.auth.lockout-minutes:15}") long lockoutMinutes) {
        this.maxFailures = maxFailures;
        this.lockoutMillis = Duration.ofMinutes(lockoutMinutes).toMillis();
    }

    public boolean isLocked(String email) {
        Failures failures = failuresByEmail.get(key(email));
        return failures != null && !failures.expired(now(), lockoutMillis) && failures.count() >= maxFailures;
    }

    public void recordFailure(String email) {
        long now = now();
        if (failuresByEmail.size() > CLEANUP_THRESHOLD) {
            failuresByEmail.values().removeIf(failures -> failures.expired(now, lockoutMillis));
        }
        failuresByEmail.merge(
                key(email),
                new Failures(1, now),
                (existing, fresh) ->
                        existing.expired(now, lockoutMillis)
                                ? fresh
                                : new Failures(existing.count() + 1, existing.windowStart()));
    }

    public void recordSuccess(String email) {
        failuresByEmail.remove(key(email));
    }

    private static String key(String email) {
        return email == null ? "" : email.trim().toLowerCase(Locale.ROOT);
    }

    private static long now() {
        return System.currentTimeMillis();
    }

    private record Failures(int count, long windowStart) {
        boolean expired(long now, long windowMillis) {
            return now - windowStart > windowMillis;
        }
    }
}
