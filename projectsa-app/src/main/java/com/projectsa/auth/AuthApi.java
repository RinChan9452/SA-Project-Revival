package com.projectsa.auth;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.projectsa.common.Auth;
import com.projectsa.common.Menu;
import com.projectsa.common.SessionUser;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;

/** F1 Register + F2 Login (outside the use cases; CLAUDE.md §3 / §6.0). */
@RestController
public class AuthApi {

    public record LoginForm(String username, String password) {
    }

    private final AuthService auth;

    public AuthApi(AuthService auth) {
        this.auth = auth;
    }

    @GetMapping("/api/skills")
    public List<Map<String, Object>> skills() {
        return auth.skills();
    }

    @PostMapping("/api/register")
    @ResponseStatus(HttpStatus.CREATED)
    public Map<String, Object> register(@RequestBody RegisterDetectBlank.RegisterForm form) {
        return Map.of("EmployeeID", auth.register(form));
    }

    @PostMapping("/api/login")
    public Map<String, Object> login(@RequestBody LoginForm form, HttpServletRequest request) {
        SessionUser u = auth.login(form.username(), form.password());
        HttpSession old = request.getSession(false);
        if (old != null) {
            old.invalidate();                       // new session id after login
        }
        request.getSession(true).setAttribute(Auth.ATTR, u);
        return me(u);
    }

    @PostMapping("/api/logout")
    public Map<String, Object> logout(HttpServletRequest request) {
        HttpSession s = request.getSession(false);
        if (s != null) {
            s.invalidate();
        }
        return Map.of("ok", true);
    }

    @GetMapping("/api/me")
    public Map<String, Object> me(HttpSession session) {
        return me(Auth.current(session));
    }

    static Map<String, Object> me(SessionUser u) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("EmployeeID", u.id());
        m.put("Name", u.fullName());
        m.put("Role", u.role());
        m.put("RoleName", Menu.ROLE_NAMES.get(u.role()));
        m.put("ProfilePicture", u.profilePicture());
        m.put("menu", Menu.forRole(u.role()));
        return m;
    }
}
