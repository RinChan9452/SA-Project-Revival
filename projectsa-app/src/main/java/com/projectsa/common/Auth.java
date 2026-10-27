package com.projectsa.common;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import org.springframework.http.HttpStatus;

import jakarta.servlet.http.HttpSession;

/** Session / role guard used by every API (CLAUDE.md §4: 401 when not logged in, 403 for the wrong role). */
public final class Auth {

    public static final String SALE = "Sale";
    public static final String PRESALE = "Presale";
    public static final String PM = "PM";
    public static final String TECH = "TechEngineer";
    public static final String ATTR = "user";

    private Auth() {
    }

    public static SessionUser current(HttpSession session) {
        Object u = session.getAttribute(ATTR);
        if (u == null) {
            throw new ApiException(HttpStatus.UNAUTHORIZED, "กรุณาเข้าสู่ระบบ");
        }
        return (SessionUser) u;
    }

    public static SessionUser require(HttpSession session, String... roles) {
        SessionUser u = current(session);
        if (roles.length > 0 && Arrays.stream(roles).noneMatch(u.role()::equals)) {
            throw new ApiException(HttpStatus.FORBIDDEN, "บทบาทของคุณไม่มีสิทธิ์ใช้งานส่วนนี้");
        }
        return u;
    }

    /** The parameter map every design query starts from: @LoginID and @Role. */
    public static Map<String, Object> params(SessionUser u) {
        Map<String, Object> p = new HashMap<>();
        p.put("LoginID", u.id());
        p.put("Role", u.role());
        return p;
    }
}
