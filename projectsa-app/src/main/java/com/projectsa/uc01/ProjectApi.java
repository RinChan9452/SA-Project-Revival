package com.projectsa.uc01;

import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.projectsa.common.Auth;

import jakarta.servlet.http.HttpSession;

/** <<API>> Project API of Sequence UC01 (actor: Sale). */
@RestController
public class ProjectApi {

    private final ProjectService service;

    public ProjectApi(ProjectService service) {
        this.service = service;
    }

    @GetMapping("/api/projects/create-form")
    public Map<String, Object> createForm(HttpSession session) {
        return service.createForm(Auth.require(session, Auth.SALE));
    }

    @PostMapping("/api/projects")
    @ResponseStatus(HttpStatus.CREATED)
    public Map<String, Object> create(@RequestBody ProjectDetectBlank.ProjectForm form, HttpSession session) {
        return Map.of("ProjectID", service.create(Auth.require(session, Auth.SALE), form));
    }
}
