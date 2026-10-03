package studdy.example.demo.auth.session;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.Duration;

@Component
public class SessionCookies {
    public static final String NAME = "acad-organize.session";
    private final boolean secure;
    private final Clock clock;

    public SessionCookies(@Value("${app.auth.cookie-secure:false}") boolean secure, Clock clock) {
        this.secure = secure;
        this.clock = clock;
    }

    public void set(BrowserSessionService.IssuedSession session, HttpServletRequest request, HttpServletResponse response) {
        var cookie = builder(session.token(), request);
        if (session.rememberMe()) cookie.maxAge(Duration.between(clock.instant(), session.expiresAt()));
        response.addHeader(HttpHeaders.SET_COOKIE, cookie.build().toString());
        response.setHeader(HttpHeaders.CACHE_CONTROL, "no-store");
    }

    public void clear(HttpServletRequest request, HttpServletResponse response) {
        response.addHeader(HttpHeaders.SET_COOKIE, builder("", request).maxAge(Duration.ZERO).build().toString());
        response.setHeader(HttpHeaders.CACHE_CONTROL, "no-store");
    }

    private ResponseCookie.ResponseCookieBuilder builder(String value, HttpServletRequest request) {
        return ResponseCookie.from(NAME, value).path("/api").httpOnly(true)
                .secure(secure || request.isSecure()).sameSite("Strict");
    }
}
