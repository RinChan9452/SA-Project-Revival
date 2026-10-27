package com.projectsa.auth;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import com.projectsa.common.Auth;
import com.projectsa.common.Check;

/** F1 Register field checks (CLAUDE.md §6.0). The same rules run in js/register.js before sending. */
public final class RegisterDetectBlank {

    /** skills = the names the Tech Engineer typed (existing or new). */
    public record RegisterForm(String firstName, String lastName, String email, String phone, String username,
                               String password, String role, List<String> skills) {
    }

    static final Set<String> ROLES = Set.of(Auth.SALE, Auth.PRESALE, Auth.PM, Auth.TECH);
    /** SKILL.SkillName is NVARCHAR(50). */
    public static final int SKILL_MAX_LENGTH = 50;
    public static final int SKILL_MAX_COUNT = 20;

    private RegisterDetectBlank() {
    }

    public static Map<String, String> check(RegisterForm f) {
        Check c = new Check()
                .required("firstName", f.firstName(), "กรุณากรอกชื่อ")
                .required("lastName", f.lastName(), "กรุณากรอกนามสกุล")
                .required("email", f.email(), "กรุณากรอกอีเมล")
                .email("email", f.email())
                .required("username", f.username(), "กรุณากรอก Username")
                .required("password", f.password(), "กรุณากรอกรหัสผ่าน")
                .rule(f.password() == null || f.password().length() >= 6, "password", "รหัสผ่านต้องมีอย่างน้อย 6 ตัวอักษร")
                // one account = exactly one role (radio buttons on the page)
                .rule(f.role() != null && ROLES.contains(f.role()), "role", "กรุณาเลือกบทบาท 1 บทบาท");
        if (Auth.TECH.equals(f.role())) {
            checkSkills(c, f.skills());
        }
        return c.errors();
    }

    /** Typed skills: trimmed, inner spaces collapsed, blanks dropped, duplicates (ignoring case) removed. */
    public static List<String> skillNames(List<String> raw) {
        List<String> out = new ArrayList<>();
        Set<String> seen = new HashSet<>();
        for (String s : raw == null ? List.<String>of() : raw) {
            String name = s == null ? "" : s.strip().replaceAll("\\s+", " ");
            if (!name.isEmpty() && seen.add(name.toUpperCase(Locale.ROOT))) {
                out.add(name);
            }
        }
        return out;
    }

    /** Shared with F3 Profile: at least 1 skill, at most 20, each at most 50 characters. */
    public static Check checkSkills(Check c, List<String> raw) {
        List<String> names = skillNames(raw);
        return c.rule(!names.isEmpty(), "skills", "Tech Engineer ต้องกรอก Skill อย่างน้อย 1 รายการ")
                .rule(names.size() <= SKILL_MAX_COUNT, "skills", "กรอก Skill ได้ไม่เกิน " + SKILL_MAX_COUNT + " รายการ")
                .rule(names.stream().allMatch(n -> n.length() <= SKILL_MAX_LENGTH), "skills",
                        "ชื่อ Skill ต้องยาวไม่เกิน " + SKILL_MAX_LENGTH + " ตัวอักษร");
    }
}
