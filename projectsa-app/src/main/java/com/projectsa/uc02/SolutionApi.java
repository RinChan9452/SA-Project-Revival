package com.projectsa.uc02;

import java.util.List;
import java.util.Map;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.projectsa.common.Auth;

import jakarta.servlet.http.HttpSession;

/** <<API>> Solution API of Sequence UC02 (actor: Presale Engineer). List: GET /api/projects?status=NewProject. */
@RestController
public class SolutionApi {

    private final SolutionService service;

    public SolutionApi(SolutionService service) {
        this.service = service;
    }

    @GetMapping("/api/projects/{id}/solution")
    public Map<String, Object> solution(@PathVariable long id, HttpSession session) {
        return service.solution(Auth.require(session, Auth.PRESALE), id);
    }

    @PostMapping(value = "/api/projects/{id}/solution", consumes = "multipart/form-data")
    public Map<String, Object> save(@PathVariable long id, @ModelAttribute SolutionDetectBlank.SolutionForm form,
                                    @RequestParam(name = "solutionFiles", required = false) List<MultipartFile> solutionFiles,
                                    @RequestParam(name = "requirementFiles", required = false) List<MultipartFile> requirementFiles,
                                    HttpSession session) {
        service.save(Auth.require(session, Auth.PRESALE), id, form, clean(solutionFiles), clean(requirementFiles));
        return Map.of("ok", true, "Status", "WaitingForAssignEngineer");
    }

    private static List<MultipartFile> clean(List<MultipartFile> l) {
        return l == null ? List.of() : l.stream().filter(f -> f != null && !f.isEmpty()).toList();
    }
}
