package com.projectsa.profile;

import java.util.Map;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.projectsa.common.Auth;
import com.projectsa.common.SessionUser;

import jakarta.servlet.http.HttpSession;

/** F3 Profile (every role; CLAUDE.md §3 / §6.0). The session user is refreshed so the header updates at once. */
@RestController
public class ProfileApi {

    private final ProfileService service;

    public ProfileApi(ProfileService service) {
        this.service = service;
    }

    @GetMapping("/api/profile")
    public Map<String, Object> profile(HttpSession session) {
        return service.profile(Auth.current(session));
    }

    @PutMapping("/api/profile")
    public Map<String, Object> info(@RequestBody ProfileService.InfoForm form, HttpSession session) {
        SessionUser u = service.updateInfo(Auth.current(session), form);
        session.setAttribute(Auth.ATTR, u);
        return Map.of("ok", true);
    }

    @PostMapping(value = "/api/profile/picture", consumes = "multipart/form-data")
    public Map<String, Object> picture(@RequestParam("picture") MultipartFile picture, HttpSession session) {
        SessionUser u = service.updatePicture(Auth.current(session), picture);
        session.setAttribute(Auth.ATTR, u);
        return Map.of("ProfilePicture", u.profilePicture());
    }

    @PutMapping("/api/profile/password")
    public Map<String, Object> password(@RequestBody ProfileService.PasswordForm form, HttpSession session) {
        service.changePassword(Auth.current(session), form);
        return Map.of("ok", true);
    }

    @PutMapping("/api/profile/skills")
    public Map<String, Object> skills(@RequestBody ProfileService.SkillsForm form, HttpSession session) {
        service.replaceSkills(Auth.require(session, Auth.TECH), form);
        return Map.of("ok", true);
    }
}
