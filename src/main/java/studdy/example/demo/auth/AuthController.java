package studdy.example.demo.auth;

import jakarta.validation.Valid;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.MediaType;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import studdy.example.demo.auth.dto.AuthResponse;
import studdy.example.demo.auth.dto.LoginRequest;
import studdy.example.demo.auth.dto.RegisterRequest;
import studdy.example.demo.user.dto.UserResponse;
import studdy.example.demo.auth.session.BrowserSessionService;
import studdy.example.demo.auth.session.SessionCookies;

@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {

    private final AuthService authService;
    private final BrowserSessionService sessions;
    private final SessionCookies cookies;

    public AuthController(AuthService authService, BrowserSessionService sessions, SessionCookies cookies) {
        this.authService = authService;
        this.sessions = sessions;
        this.cookies = cookies;
    }

    @PostMapping("/register")
    @ResponseStatus(HttpStatus.CREATED)
    public UserResponse register(@Valid @RequestBody RegisterRequest request) {
        return authService.register(request);
    }

    @PostMapping("/login")
    public AuthResponse login(@Valid @RequestBody LoginRequest request,
                              @CookieValue(name = SessionCookies.NAME, required = false) String previousToken,
                              HttpServletRequest httpRequest, HttpServletResponse response) {
        var auth = authService.login(request);
        cookies.set(sessions.createAuthenticated(auth.accessToken(), request.rememberMe(), previousToken), httpRequest, response);
        return auth;
    }

    @PostMapping(value = "/refresh", consumes = MediaType.APPLICATION_JSON_VALUE)
    public BrowserSessionService.SessionResponse refresh(
            @CookieValue(name = SessionCookies.NAME, required = false) String token,
            @RequestHeader(name = "X-Session-Request", required = false) String sessionRequest,
            HttpServletRequest request, HttpServletResponse response) {
        requireSameOriginRequest(sessionRequest, request);
        response.setHeader("Cache-Control", "no-store");
        try {
            return sessions.refresh(token);
        } catch (ResponseStatusException exception) {
            cookies.clear(request, response);
            throw exception;
        }
    }

    @PostMapping(value = "/logout", consumes = MediaType.APPLICATION_JSON_VALUE)
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void logout(@CookieValue(name = SessionCookies.NAME, required = false) String token,
                       @RequestHeader(name = "X-Session-Request", required = false) String sessionRequest,
                       HttpServletRequest request, HttpServletResponse response) {
        requireSameOriginRequest(sessionRequest, request);
        sessions.revoke(token);
        cookies.clear(request, response);
    }

    private void requireSameOriginRequest(String header, HttpServletRequest request) {
        // Cabeçalho não simples exige preflight CORS; não autorizamos origens externas.
        if (!"1".equals(header) || "cross-site".equals(request.getHeader("Sec-Fetch-Site"))) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Origem da solicitação inválida.");
        }
    }
}
