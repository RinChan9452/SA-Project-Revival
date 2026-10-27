package com.projectsa.uc04;

import java.util.List;
import java.util.Map;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.projectsa.common.Auth;

import jakarta.servlet.http.HttpSession;

/** <<API>> Assign API of Sequence UC04 (actor: PM). List: GET /api/projects?status=WaitingForAssignEngineer. */
@RestController
public class AssignApi {

    private final AssignService service;

    public AssignApi(AssignService service) {
        this.service = service;
    }

    @GetMapping("/api/projects/{id}/assign-form")
    public Map<String, Object> assignForm(@PathVariable long id, HttpSession session) {
        return service.assignForm(Auth.require(session, Auth.PM), id);
    }

    @GetMapping("/api/engineers")
    public List<Map<String, Object>> engineers(@RequestParam long skill, HttpSession session) {
        return service.filter(Auth.require(session, Auth.PM), skill);
    }

    @PostMapping("/api/projects/{id}/assignments")
    public Map<String, Object> assign(@PathVariable long id, @RequestBody AssignDetectBlank.AssignForm form, HttpSession session) {
        service.assign(Auth.require(session, Auth.PM), id, form);
        return Map.of("ok", true, "Status", "Working");
    }
}
