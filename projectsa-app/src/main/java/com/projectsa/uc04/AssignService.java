package com.projectsa.uc04;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.projectsa.common.ApiException;
import com.projectsa.common.Auth;
import com.projectsa.common.ProjectListApi;
import com.projectsa.common.SessionUser;
import com.projectsa.common.Sql;

/** UC04 Assign Engineer to Project – Query 3.1–3.3, 5.1, 8.1, 8.2, 9.1, 10.1 (list: 1.1). */
@Service
public class AssignService {

    private final Sql sql;
    private final ProjectListApi lists;

    public AssignService(Sql sql, ProjectListApi lists) {
        this.sql = sql;
        this.lists = lists;
    }

    /** Step 3: solution + every Tech Engineer (skills, active projects, already in this project) + skill list. */
    public Map<String, Object> assignForm(SessionUser u, long projectId) {
        lists.requireInList("UC04", u, projectId);
        Map<String, Object> p = Auth.params(u);
        p.put("ProjectID", projectId);
        Map<String, Object> out = new LinkedHashMap<>();
        out.put("project", sql.row("UC04", "3.1", p));                           // 3.1
        out.put("engineers", sql.rows("UC04", "3.2", p));                         // 3.2
        out.put("skills", sql.rows("UC04", "3.3", p));                            // 3.3
        return out;
    }

    /** Step 5: engineers with the chosen skill (empty list → Alt 5a "ไม่พบ Engineer"). */
    public List<Map<String, Object>> filter(SessionUser u, long skillId) {
        Map<String, Object> p = Auth.params(u);
        p.put("SkillID", skillId);
        return sql.rows("UC04", "5.1", p);                                         // 5.1
    }

    /** Steps 7–10 in one transaction. */
    @Transactional
    public void assign(SessionUser u, long projectId, AssignDetectBlank.AssignForm f) {
        ApiException.throwIfAny(AssignDetectBlank.check(f));                       // Step 7 (Alt 7a)
        lists.requireInList("UC04", u, projectId);                                 // still WaitingForAssignEngineer
        Map<String, Object> p = Auth.params(u);
        p.put("ProjectID", projectId);
        Set<Long> engineers = sql.rows("UC04", "3.2", p).stream()
                .map(r -> ((Number) r.get("EmployeeID")).longValue()).collect(Collectors.toSet());
        if (!engineers.containsAll(f.engineerIds())) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "ข้อมูลไม่ครบหรือไม่ถูกต้อง",
                    Map.of("engineerIds", "เลือกได้เฉพาะ Tech Engineer"));
        }
        for (Long e : f.engineerIds().stream().distinct().toList()) {
            p.put("EngineerID", e);
            sql.update("UC04", "8.1", p);                                          // 8.1 (skips already assigned)
        }
        sql.update("UC04", "8.2", p);                                              // 8.2 PM = the login
        if (sql.update("UC04", "9.1", p) != 1) {                                   // 9.1 Working
            throw new ApiException(HttpStatus.CONFLICT, "อัปเดตสถานะไม่สำเร็จ");
        }
        sql.update("UC04", "10.1", p);                                             // 10.1 notify engineers
    }
}
