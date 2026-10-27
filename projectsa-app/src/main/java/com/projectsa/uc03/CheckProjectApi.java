package com.projectsa.uc03;

import java.nio.charset.StandardCharsets;
import java.util.Map;

import org.springframework.core.io.Resource;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.MediaTypeFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.projectsa.common.Auth;
import com.projectsa.common.FileStore;

import jakarta.servlet.http.HttpSession;

/** <<API>> Project API of Sequence UC03 (every role). The list is GET /api/projects (ProjectListApi). */
@RestController
public class CheckProjectApi {

    private final CheckProjectService service;
    private final FileStore files;

    public CheckProjectApi(CheckProjectService service, FileStore files) {
        this.service = service;
        this.files = files;
    }

    @GetMapping("/api/projects/{id}")
    public Map<String, Object> detail(@PathVariable long id, HttpSession session) {
        return service.detail(Auth.current(session), id);
    }

    @GetMapping("/api/projects/{id}/documents/{documentId}")
    public ResponseEntity<Resource> document(@PathVariable long id, @PathVariable long documentId, HttpSession session) {
        return send(service.document(Auth.current(session), id, documentId));
    }

    @GetMapping("/api/projects/{id}/requirements/{requirementId}/attachment")
    public ResponseEntity<Resource> attachment(@PathVariable long id, @PathVariable long requirementId,
                                               @RequestParam String file, HttpSession session) {
        return send(service.attachment(Auth.current(session), id, requirementId, file));
    }

    private ResponseEntity<Resource> send(CheckProjectService.Download d) {
        Resource r = files.load(d.path());
        MediaType type = MediaTypeFactory.getMediaType(d.fileName()).orElse(MediaType.APPLICATION_OCTET_STREAM);
        return ResponseEntity.ok().contentType(type)
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        ContentDisposition.attachment().filename(d.fileName(), StandardCharsets.UTF_8).build().toString())
                .body(r);
    }
}
