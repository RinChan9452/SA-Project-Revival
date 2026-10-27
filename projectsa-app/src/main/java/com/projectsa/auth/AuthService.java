package com.projectsa.auth;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.projectsa.common.ApiException;
import com.projectsa.common.Auth;
import com.projectsa.common.Check;
import com.projectsa.common.SessionUser;
import com.projectsa.common.Sql;

/** F1 Register (Query R.1–R.6) and F2 Login (Query L.1) of ProjectSA_SQL.sql. */
@Service
public class AuthService {

    public static final BCryptPasswordEncoder BCRYPT = new BCryptPasswordEncoder();

    private final Sql sql;

    public AuthService(Sql sql) {
        this.sql = sql;
    }

    public List<Map<String, Object>> skills() {
        return sql.rows("F1", "R.1", Map.of());                                   // R.1
    }

    @Transactional
    public long register(RegisterDetectBlank.RegisterForm f) {
        ApiException.throwIfAny(RegisterDetectBlank.check(f));
        Map<String, Object> p = new HashMap<>();
        p.put("Username", f.username().strip());
        p.put("Email", f.email().strip());
        if (sql.count("F1", "R.2", p) > 0) {                                       // R.2
            throw new ApiException(HttpStatus.CONFLICT, "Username หรืออีเมลนี้ถูกใช้แล้ว",
                    Map.of("username", "Username หรืออีเมลนี้ถูกใช้แล้ว"));
        }
        p.put("FirstName", f.firstName().strip());
        p.put("LastName", f.lastName().strip());
        p.put("Phone", Check.blank(f.phone()) ? null : f.phone().strip());
        p.put("PasswordHash", BCRYPT.encode(f.password()));
        p.put("Role", f.role());
        long id = sql.insert("F1", "R.3", p);                                      // R.3 (one role)
        if (Auth.TECH.equals(f.role())) {
            saveSkills(id, f.skills());
        }
        return id;
    }

    /** Typed skill names → R.5 (existing, ignoring case) or R.6 (new SKILL row), then R.4. Also used by F3 Profile. */
    public void saveSkills(long employeeId, List<String> typed) {
        for (String name : RegisterDetectBlank.skillNames(typed)) {
            Map<String, Object> p = Map.of("SkillName", name);
            Object found = sql.value("F1", "R.5", p);                              // R.5
            long skillId = found != null ? ((Number) found).longValue() : sql.insert("F1", "R.6", p);   // R.6
            sql.update("F1", "R.4", Map.of("EmployeeID", employeeId, "SkillID", skillId));  // R.4
        }
    }

    public SessionUser login(String username, String password) {
        Map<String, Object> row = Check.blank(username) ? null
                : sql.row("F2", "L.1", Map.of("Username", username.strip()));     // L.1
        if (row == null || password == null || !BCRYPT.matches(password, (String) row.get("Password"))) {
            throw new ApiException(HttpStatus.UNAUTHORIZED, "Username หรือรหัสผ่านไม่ถูกต้อง");
        }
        return new SessionUser(((Number) row.get("EmployeeID")).longValue(), (String) row.get("Role"),
                (String) row.get("FirstName"), (String) row.get("LastName"), (String) row.get("ProfilePicture"));
    }
}
