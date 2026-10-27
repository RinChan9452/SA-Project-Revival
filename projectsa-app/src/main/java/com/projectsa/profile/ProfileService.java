package com.projectsa.profile;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import com.projectsa.auth.AuthService;
import com.projectsa.auth.RegisterDetectBlank;
import com.projectsa.common.ApiException;
import com.projectsa.common.Auth;
import com.projectsa.common.Check;
import com.projectsa.common.FileCheck;
import com.projectsa.common.FileStore;
import com.projectsa.common.SessionUser;
import com.projectsa.common.Sql;

/** F3 Profile – Query P.1–P.7 (+ L.1 to check the current password, R.1 / R.4–R.6 for skills). Role / Username are read-only. */
@Service
public class ProfileService {

    public record InfoForm(String firstName, String lastName, String email, String phone) {
    }

    public record PasswordForm(String currentPassword, String newPassword) {
    }

    public record SkillsForm(List<String> skills) {
    }

    private final Sql sql;
    private final FileStore files;
    private final AuthService auth;

    public ProfileService(Sql sql, FileStore files, AuthService auth) {
        this.sql = sql;
        this.files = files;
        this.auth = auth;
    }

    public Map<String, Object> profile(SessionUser u) {
        Map<String, Object> p = Auth.params(u);
        Map<String, Object> out = new LinkedHashMap<>();
        out.put("me", sql.row("F3", "P.1", p));                                         // P.1
        if (Auth.TECH.equals(u.role())) {
            out.put("skills", sql.rows("F3", "P.2", p));                                // P.2
            out.put("allSkills", sql.rows("F1", "R.1", p));                             // R.1 (suggestions)
        }
        return out;
    }

    /** P.3 + P.4 → the refreshed session user. */
    public SessionUser updateInfo(SessionUser u, InfoForm f) {
        new Check().required("firstName", f.firstName(), "กรุณากรอกชื่อ")
                .required("lastName", f.lastName(), "กรุณากรอกนามสกุล")
                .required("email", f.email(), "กรุณากรอกอีเมล")
                .email("email", f.email())
                .throwIfAny();
        Map<String, Object> p = Auth.params(u);
        p.put("Email", f.email().strip());
        if (sql.count("F3", "P.3", p) > 0) {                                           // P.3
            throw new ApiException(HttpStatus.CONFLICT, "อีเมลนี้ถูกใช้แล้ว", Map.of("email", "อีเมลนี้ถูกใช้แล้ว"));
        }
        p.put("FirstName", f.firstName().strip());
        p.put("LastName", f.lastName().strip());
        p.put("Phone", Check.blank(f.phone()) ? null : f.phone().strip());
        sql.update("F3", "P.4", p);                                                     // P.4
        return new SessionUser(u.id(), u.role(), f.firstName().strip(), f.lastName().strip(), u.profilePicture());
    }

    /** P.5: JPG / PNG ≤ 5 MB under ./uploads/profile/ → the refreshed session user. */
    public SessionUser updatePicture(SessionUser u, MultipartFile picture) {
        String why = FileCheck.PROFILE_PICTURE.problem(picture);
        if (why != null) {
            throw new ApiException(HttpStatus.BAD_REQUEST, why, Map.of("picture", why));
        }
        String url = "/uploads/" + files.save(picture, "profile");
        Map<String, Object> p = Auth.params(u);
        p.put("ProfilePicture", url);
        sql.update("F3", "P.5", p);                                                     // P.5
        return new SessionUser(u.id(), u.role(), u.firstName(), u.lastName(), url);
    }

    /** L.1 (current password) + P.6. */
    public void changePassword(SessionUser u, PasswordForm f) {
        new Check().required("currentPassword", f.currentPassword(), "กรุณากรอกรหัสผ่านเดิม")
                .required("newPassword", f.newPassword(), "กรุณากรอกรหัสผ่านใหม่")
                .rule(f.newPassword() == null || f.newPassword().length() >= 6, "newPassword", "รหัสผ่านต้องมีอย่างน้อย 6 ตัวอักษร")
                .throwIfAny();
        Map<String, Object> me = sql.row("F3", "P.1", Auth.params(u));
        Map<String, Object> login = sql.row("F2", "L.1", Map.of("Username", me.get("Username")));        // L.1
        if (!AuthService.BCRYPT.matches(f.currentPassword(), (String) login.get("Password"))) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "รหัสผ่านเดิมไม่ถูกต้อง", Map.of("currentPassword", "รหัสผ่านเดิมไม่ถูกต้อง"));
        }
        Map<String, Object> p = Auth.params(u);
        p.put("PasswordHash", AuthService.BCRYPT.encode(f.newPassword()));
        sql.update("F3", "P.6", p);                                                     // P.6
    }

    /** Tech Engineer only: P.7, then R.5 / R.6 + R.4 per typed skill, one transaction, at least 1 skill. */
    @Transactional
    public void replaceSkills(SessionUser u, SkillsForm f) {
        RegisterDetectBlank.checkSkills(new Check(), f.skills()).throwIfAny();
        sql.update("F3", "P.7", Auth.params(u));                                        // P.7
        auth.saveSkills(u.id(), f.skills());                                            // R.5 / R.6 + R.4
    }
}
