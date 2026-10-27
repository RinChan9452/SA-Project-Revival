package com.projectsa.uc07;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.projectsa.common.ApiException;
import com.projectsa.common.Auth;
import com.projectsa.common.FileStore;
import com.projectsa.common.ProjectListApi;
import com.projectsa.common.SessionUser;
import com.projectsa.common.Sql;

/** UC07 Approve New Project Requirement – Query 3.1, 5.1, 6.1, 6.2, 9.1, 10.1, 11.1–11.4 (list: 1.1). */
@Service
public class ApproveService {

    private final Sql sql;
    private final ProjectListApi lists;

    public ApproveService(Sql sql, ProjectListApi lists) {
        this.sql = sql;
        this.lists = lists;
    }

    /** Step 3: pending requirements of one of my WaitingForApprove projects. */
    public List<Map<String, Object>> pending(SessionUser u, long projectId) {
        lists.requireInList("UC07", u, projectId);
        Map<String, Object> p = Auth.params(u);
        p.put("ProjectID", projectId);
        return sql.rows("UC07", "3.1", p);                                              // 3.1
    }

    private Map<String, Object> mine(SessionUser u, long projectId, long requirementId) {
        boolean listed = pending(u, projectId).stream()
                .anyMatch(r -> ((Number) r.get("RequirementID")).longValue() == requirementId);
        if (!listed) {
            throw new ApiException(HttpStatus.CONFLICT, "Requirement นี้ถูกพิจารณาไปแล้ว");        // Alt 5a
        }
        Map<String, Object> p = Auth.params(u);
        p.put("ProjectID", projectId);
        p.put("RequirementID", requirementId);
        return p;
    }

    /** Steps 5–6: still Pending (5.1), details (6.1) and attachments (6.2). */
    public Map<String, Object> detail(SessionUser u, long projectId, long requirementId) {
        Map<String, Object> p = mine(u, projectId, requirementId);
        if (!"Pending".equals(sql.value("UC07", "5.1", p))) {                          // 5.1
            throw new ApiException(HttpStatus.CONFLICT, "Requirement นี้ถูกพิจารณาไปแล้ว");
        }
        Map<String, Object> out = new LinkedHashMap<>();
        out.put("requirement", sql.row("UC07", "6.1", p));                             // 6.1
        List<Map<String, Object>> att = sql.rows("UC07", "6.2", p);                    // 6.2
        att.forEach(a -> a.put("FileName", FileStore.displayName((String) a.get("AttachmentFile"))));
        out.put("attachments", att);
        return out;
    }

    /** Steps 8–11 in one transaction. Returns the project status afterwards. */
    @Transactional
    public String decide(SessionUser u, long requirementId, ApproveDetectBlank.DecisionForm f) {
        ApiException.throwIfAny(ApproveDetectBlank.check(f));                          // Step 8 (Alt 8a)
        Map<String, Object> p = mine(u, f.projectId(), requirementId);
        p.put("Decision", f.decision());
        p.put("RejectReason", "Rejected".equals(f.decision()) ? f.rejectReason().strip() : null);
        if (sql.update("UC07", "9.1", p) != 1) {                                        // 9.1 (guard Status = 'Pending')
            throw new ApiException(HttpStatus.CONFLICT, "Requirement นี้ถูกพิจารณาไปแล้ว");
        }
        sql.update("UC07", "10.1", p);                                                  // 10.1 notify the PM
        if (sql.count("UC07", "11.1", p) > 0) {                                         // 11.1 (Alt 11a)
            return "WaitingForApprove";
        }
        if (sql.count("UC07", "11.2", p) > 0) {                                         // 11.2
            sql.update("UC07", "11.3", p);                                              // 11.3 NewProject + notify Presale
            return "NewProject";
        }
        sql.update("UC07", "11.4", p);                                                  // 11.4 Finish
        return "Finish";
    }
}
