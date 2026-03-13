package nz.clem.blog.controller;

import jakarta.servlet.http.HttpServletRequest;
import nz.clem.blog.dto.LoginRequest;
import nz.clem.blog.dto.LoginResponse;
import nz.clem.blog.security.JwtUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    @Autowired
    private AuthenticationManager authenticationManager;

    @Autowired
    private JwtUtil jwtUtil;

    private static final int MAX_ATTEMPTS = 5;
    private static final long WINDOW_SECONDS = 900; // 15 minutes
    private final Map<String, Deque<Instant>> loginAttempts = new ConcurrentHashMap<>();

    @PostMapping("/login")
    public ResponseEntity<LoginResponse> login(@RequestBody LoginRequest loginRequest,
                                               HttpServletRequest request) {
        String ip = getClientIp(request);

        if (isRateLimited(ip)) {
            return ResponseEntity.status(429).body(new LoginResponse(null));
        }

        try {
            Authentication authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(
                            loginRequest.getUsername(),
                            loginRequest.getPassword()
                    )
            );

            // Successful login — clear the attempt history for this IP
            loginAttempts.remove(ip);
            String jwt = jwtUtil.generateToken(authentication.getName());
            return ResponseEntity.ok(new LoginResponse(jwt));
        } catch (AuthenticationException e) {
            recordAttempt(ip);
            return ResponseEntity.status(401).body(new LoginResponse(null));
        }
    }

    private boolean isRateLimited(String ip) {
        Deque<Instant> attempts = loginAttempts.getOrDefault(ip, new ArrayDeque<>());
        pruneOld(attempts);
        return attempts.size() >= MAX_ATTEMPTS;
    }

    private void recordAttempt(String ip) {
        loginAttempts.compute(ip, (k, deque) -> {
            if (deque == null) deque = new ArrayDeque<>();
            pruneOld(deque);
            deque.addLast(Instant.now());
            return deque;
        });
    }

    private void pruneOld(Deque<Instant> deque) {
        Instant cutoff = Instant.now().minusSeconds(WINDOW_SECONDS);
        while (!deque.isEmpty() && deque.peekFirst().isBefore(cutoff)) {
            deque.pollFirst();
        }
    }

    private String getClientIp(HttpServletRequest request) {
        String forwarded = request.getHeader("X-Forwarded-For");
        if (forwarded != null && !forwarded.isBlank()) {
            return forwarded.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }
}
