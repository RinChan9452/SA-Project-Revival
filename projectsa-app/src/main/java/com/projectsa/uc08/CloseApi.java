package com.projectsa.uc08;

import java.util.Map;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

import com.projectsa.common.Auth;

import jakarta.servlet.http.HttpSession;

/** <<API>> Close API of Sequence UC08 (actor: PM). List: GET /api/projects?status=Testing&pm=me. */
@RestController
public class CloseApi {

    private final CloseService service;

    public CloseApi(CloseService service) {
        this.service = service;
    }

    @GetMapping("/api/projects/{id}/close-form")
    public Map<String, Object> closeForm(@PathVariable long id, HttpSession session) {
        return service.closeForm(Auth.require(session, Auth.PM), id);
    }

    @PostMapping("/api/projects/{id}/close")
    public Map<String, Object> close(@PathVariable long id, HttpSession session) {
        service.close(Auth.require(session, Auth.PM), id);
        return Map.of("ok", true, "Status", "Finish");
    }
}
