package com.projectsa.uc06;

import java.util.List;
import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.projectsa.common.Auth;

import jakarta.servlet.http.HttpSession;

/** <<API>> Requirement API of Sequence UC06 (actor: PM). List: GET /api/projects?status=Testing,WaitingForApprove. */
@RestController
public class RequirementApi {

    private final RequirementService service;

    public RequirementApi(RequirementService service) {
        this.service = service;
    }

    @GetMapping("/api/projects/{id}/requirement-form")
    public Map<String, Object> requirementForm(@PathVariable long id, HttpSession session) {
        return service.requirementForm(Auth.require(session, Auth.PM), id);
    }

    @PostMapping(value = "/api/projects/{id}/requirements", consumes = "multipart/form-data")
    @ResponseStatus(HttpStatus.CREATED)
    public Map<String, Object> submit(@PathVariable long id, @ModelAttribute RequirementDetectBlank.RequirementForm form,
                                      @RequestParam(name = "attachments", required = false) List<MultipartFile> attachments,
                                      HttpSession session) {
        List<MultipartFile> files = attachments == null ? List.of()
                : attachments.stream().filter(f -> f != null && !f.isEmpty()).toList();
        return Map.of("RequirementID", service.submit(Auth.require(session, Auth.PM), id, form, files),
                "Status", "WaitingForApprove");
    }
}
