package studdy.example.demo.security;

import io.jsonwebtoken.JwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import studdy.example.demo.auth.session.BrowserSessionService;
import studdy.example.demo.user.UserRepository;

import java.io.IOException;
import java.util.List;
import java.util.UUID;

@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtService jwtService;
    private final UserRepository userRepository;
    private final BrowserSessionService browserSessions;

    public JwtAuthenticationFilter(JwtService jwtService, UserRepository userRepository,
                                   BrowserSessionService browserSessions) {
        this.jwtService = jwtService;
        this.userRepository = userRepository;
        this.browserSessions = browserSessions;
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {

        String authorization = request.getHeader("Authorization");

        if (authorization != null && authorization.startsWith("Bearer ")) {
            try {
                String token = authorization.substring(7);
                JwtService.TokenClaims claims = jwtService.parse(token);

                // Todo token é emitido para uma sessão de navegador (claim "sid"): logout, novo login ou
                // expiração o invalidam. Token sem sid não autentica.
                if (claims.sessionId() == null || !browserSessions.isActive(claims.sessionId(), claims.userId())) {
                    filterChain.doFilter(request, response);
                    return;
                }

                // Conta excluída ou senha trocada depois da emissão: o token não autentica mais.
                userRepository.findById(claims.userId())
                        .filter(user -> user.acceptsTokenIssuedAt(claims.issuedAt()))
                        .ifPresent(user -> authenticate(claims.userId(), request));
            } catch (JwtException | IllegalArgumentException ignored) {
                // Token inválido: a rota protegida retornará 401.
            }
        }

        filterChain.doFilter(request, response);
    }

    private void authenticate(UUID userId, HttpServletRequest request) {
        var authentication = new UsernamePasswordAuthenticationToken(userId, null, List.of());

        authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));

        SecurityContextHolder.getContext().setAuthentication(authentication);
    }
}