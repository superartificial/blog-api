package nz.clem.blog.security;

import jakarta.servlet.FilterChain;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetailsService;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class JwtAuthenticationFilterTest {

    @Mock
    private JwtUtil jwtUtil;

    @Mock
    private UserDetailsService userDetailsService;

    @InjectMocks
    private JwtAuthenticationFilter filter;

    // MockHttpServletRequest/Response are Spring's built-in fake HTTP objects —
    // no need to mock them, just instantiate and configure.
    private final MockHttpServletRequest request = new MockHttpServletRequest();
    private final MockHttpServletResponse response = new MockHttpServletResponse();
    private final FilterChain chain = mock(FilterChain.class);

    @AfterEach
    void clearSecurityContext() {
        // SecurityContextHolder is a static/global — always reset it after each test
        // so authentication set in one test doesn't leak into the next.
        SecurityContextHolder.clearContext();
    }

    @Test
    void validToken_setsAuthenticationAndContinuesChain() throws Exception {
        // given
        request.addHeader("Authorization", "Bearer valid-token");

        User userDetails = new User("alice", "hashed", List.of(new SimpleGrantedAuthority("ROLE_ADMIN")));
        when(jwtUtil.validateToken("valid-token")).thenReturn(true);
        when(jwtUtil.getUsernameFromToken("valid-token")).thenReturn("alice");
        when(userDetailsService.loadUserByUsername("alice")).thenReturn(userDetails);

        // when
        filter.doFilter(request, response, chain);

        // then — authentication is set in the security context
        var authentication = SecurityContextHolder.getContext().getAuthentication();
        assertThat(authentication).isNotNull();
        assertThat(authentication.getName()).isEqualTo("alice");

        // and the request was passed along the chain
        verify(chain).doFilter(request, response);
    }

    @Test
    void noAuthorizationHeader_skipsAuthAndContinuesChain() throws Exception {
        // given — no header set on request

        // when
        filter.doFilter(request, response, chain);

        // then — security context untouched
        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
        verify(chain).doFilter(request, response);
        verifyNoInteractions(jwtUtil, userDetailsService);
    }

    @Test
    void headerWithoutBearerPrefix_skipsAuthAndContinuesChain() throws Exception {
        // given — malformed header (no "Bearer " prefix)
        request.addHeader("Authorization", "valid-token");

        // when
        filter.doFilter(request, response, chain);

        // then
        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
        verify(chain).doFilter(request, response);
        verifyNoInteractions(jwtUtil, userDetailsService);
    }

    @Test
    void invalidToken_skipsAuthAndStillContinuesChain() throws Exception {
        // given — token present but validation fails
        request.addHeader("Authorization", "Bearer bad-token");
        when(jwtUtil.validateToken("bad-token")).thenReturn(false);

        // when
        filter.doFilter(request, response, chain);

        // then — no authentication set, but chain must still be called
        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
        verify(chain).doFilter(request, response);
    }
}
