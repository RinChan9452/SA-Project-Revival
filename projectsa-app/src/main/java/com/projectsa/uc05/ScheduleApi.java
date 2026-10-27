package com.projectsa.uc05;

import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.projectsa.common.Auth;

import jakarta.servlet.http.HttpSession;

/** <<API>> Schedule API of Sequence UC05 (actor: Tech Engineer). List: GET /api/projects?assigned=me. */
@RestController
public class ScheduleApi {

    private final ScheduleService service;

    public ScheduleApi(ScheduleService service) {
        this.service = service;
    }

    @GetMapping("/api/projects/{id}/schedule-form")
    public Map<String, Object> scheduleForm(@PathVariable long id, HttpSession session) {
        return service.scheduleForm(Auth.require(session, Auth.TECH), id);
    }

    @PostMapping("/api/projects/{id}/schedules")
    @ResponseStatus(HttpStatus.CREATED)
    public Map<String, Object> save(@PathVariable long id, @RequestBody ScheduleDetectBlank.ScheduleForm form, HttpSession session) {
        return Map.of("ScheduleID", service.save(Auth.require(session, Auth.TECH), id, form), "Status", "Testing");
    }
}
