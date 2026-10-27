package com.projectsa.common;

import java.util.List;
import java.util.Map;
import java.util.Objects;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import jakarta.servlet.http.HttpSession;

/**
 * GET /api/projects – the <<Boundary>> ProjectList Form of every use case. The sequence diagrams give each UC
 * its own filter; each filter runs that UC's Query 1.1 for the roles allowed to use it.
 */
@RestController
public class ProjectListApi {

    /** one row per sequence-diagram call: filter → (use case, role) */
    private record Route(String uc, String status, String assigned, String pm, String role) {
    }

    private static final List<Route> ROUTES = List.of(
            new Route("UC02", "NewProject", null, null, Auth.PRESALE),
            new Route("UC04", "WaitingForAssignEngineer", null, null, Auth.PM),
            new Route("UC05", null, "me", null, Auth.TECH),
            new Route("UC06", "Testing,WaitingForApprove", null, null, Auth.PM),
            new Route("UC07", "WaitingForApprove", null, null, Auth.SALE),
            new Route("UC08", "Testing", null, "me", Auth.PM));

    private final Sql sql;

    public ProjectListApi(Sql sql) {
        this.sql = sql;
    }

    @GetMapping("/api/projects")
    public List<Map<String, Object>> list(@RequestParam(required = false) String status,
                                          @RequestParam(required = false) String assigned,
                                          @RequestParam(required = false) String pm,
                                          @RequestParam(required = false) String keyword,
                                          HttpSession session) {
        SessionUser u = Auth.current(session);
        if (status == null && assigned == null && pm == null) {
            Map<String, Object> p = Auth.params(u);                         // UC03 Query 1.1, every role
            p.put("Keyword", Check.blank(keyword) ? null : keyword.strip());
            return sql.rows("UC03", "1.1", p);
        }
        Route r = ROUTES.stream()
                .filter(x -> Objects.equals(x.status, status) && Objects.equals(x.assigned, assigned) && Objects.equals(x.pm, pm))
                .findFirst()
                .orElseThrow(() -> new ApiException(HttpStatus.BAD_REQUEST, "ตัวกรองรายการ Project ไม่ถูกต้อง"));
        return sql.rows(r.uc, "1.1", Auth.params(Auth.require(session, r.role)));
    }

    /** Server-side re-check for write endpoints: is this project still in the UC's Query 1.1 list for this user? */
    public boolean inList(String uc, SessionUser u, long projectId) {
        return sql.rows(uc, "1.1", Auth.params(u)).stream()
                .anyMatch(row -> ((Number) row.get("ProjectID")).longValue() == projectId);
    }

    public void requireInList(String uc, SessionUser u, long projectId) {
        if (!inList(uc, u, projectId)) {
            throw new ApiException(HttpStatus.CONFLICT, "Project นี้ไม่อยู่ในสถานะที่ทำรายการนี้ได้แล้ว หรือคุณไม่ได้รับผิดชอบ Project นี้");
        }
    }
}
