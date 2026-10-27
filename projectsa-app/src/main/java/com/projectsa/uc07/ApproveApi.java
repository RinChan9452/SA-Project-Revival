package com.projectsa.uc07;

import java.util.List;
import java.util.Map;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.projectsa.common.Auth;

import jakarta.servlet.http.HttpSession;

/**
 * <<API>> Approve API of Sequence UC07 (actor: Sale). List: GET /api/projects?status=WaitingForApprove.
 * The requirement endpoints also take the project id (?project= / body.projectId): no design query maps a
 * RequirementID to its project, so ownership is checked with Query 1.1 + 3.1 instead.
 */
@RestController
public class ApproveApi {

    private final ApproveService service;

    public ApproveApi(ApproveService service) {
        this.service = service;
    }

    @GetMapping("/api/projects/{id}/requirements")
    public List<Map<String, Object>> pending(@PathVariable long id, @RequestParam(defaultValue = "Pending") String status,
                                             HttpSession session) {
        return service.pending(Auth.require(session, Auth.SALE), id);
    }

    @GetMapping("/api/requirements/{requirementId}")
    public Map<String, Object> detail(@PathVariable long requirementId, @RequestParam long project, HttpSession session) {
        return service.detail(Auth.require(session, Auth.SALE), project, requirementId);
    }

    @PutMapping("/api/requirements/{requirementId}/decision")
    public Map<String, Object> decide(@PathVariable long requirementId, @RequestBody ApproveDetectBlank.DecisionForm form,
                                      HttpSession session) {
        return Map.of("ProjectStatus", service.decide(Auth.require(session, Auth.SALE), requirementId, form));
    }
}
