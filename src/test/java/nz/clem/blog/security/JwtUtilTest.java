package nz.clem.blog.security;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;

class JwtUtilTest {

    private JwtUtil jwtUtil;

    @BeforeEach
    void setUp() {
        jwtUtil = new JwtUtil();
        // Inject values that would normally come from application.properties
        ReflectionTestUtils.setField(jwtUtil, "jwtSecret", "test-secret-key-that-is-long-enough-for-hmac-sha512-algorithm-requirements-minimum-64-bytes");
        ReflectionTestUtils.setField(jwtUtil, "jwtExpiration", 86400000L);
    }

    @Test
    void generateToken_containsCorrectUsername() {
        String token = jwtUtil.generateToken("alice");

        String extractedUsername = jwtUtil.getUsernameFromToken(token);

        assertThat(extractedUsername).isEqualTo("alice");
    }

    @Test
    void validateToken_returnsTrueForValidToken() {
        String token = jwtUtil.generateToken("alice");

        assertThat(jwtUtil.validateToken(token)).isTrue();
    }

    @Test
    void validateToken_returnsFalseForGarbageToken() {
        assertThat(jwtUtil.validateToken("this.is.not.a.jwt")).isFalse();
    }
}
