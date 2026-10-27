package com.projectsa.uc06;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import com.projectsa.common.ApiException;
import com.projectsa.common.Auth;
import com.projectsa.common.EmailStub;
import com.projectsa.common.FileCheck;
import com.projectsa.common.FileStore;
import com.projectsa.common.ProjectListApi;
import com.projectsa.common.SessionUser;
import com.projectsa.common.Sql;

/** UC06 Add Customer New Requirement – Query 3.1, 3.2, 8.1, 8.2, 9.1, 10.1 (list: 1.1). */
@Service
public class RequirementService {

    private final Sql sql;
    private final ProjectListApi lists;
    private final FileStore files;
    private final EmailStub email;

    public RequirementService(Sql sql, ProjectListApi lists, FileStore files, EmailStub email) {
        this.sql = sql;
        this.lists = lists;
        this.files = files;
        this.email = email;
    }

    /** Step 3: project details 3.1–3.7. */
    public Map<String, Object> requirementForm(SessionUser u, long projectId) {
        lists.requireInList("UC06", u, projectId);
        Map<String, Object> p = Auth.params(u);
        p.put("ProjectID", projectId);
        Map<String, Object> out = new LinkedHashMap<>();
        out.put("project", sql.row("UC06", "3.1", p));                         // 3.1
        out.put("engineers", sql.rows("UC06", "3.2", p));                      // 3.2
        return out;
    }

    /** Steps 5–10 in one transaction (+ Sale email, which the demo only logs). */
    @Transactional
    public long submit(SessionUser u, long projectId, RequirementDetectBlank.RequirementForm f, List<MultipartFile> attachments) {
        Map<String, String> errors = new LinkedHashMap<>();
        LocalDate due = RequirementDetectBlank.check(f, LocalDate.now(), errors);              // Step 5 (5a)
        List<String> bad = new ArrayList<>();                                                  // Step 7 (6a: none = skip)
        for (MultipartFile a : attachments) {
            String why = FileCheck.REQUIREMENT.problem(a);
            if (why != null) bad.add(why);
        }
        if (!bad.isEmpty()) {
            errors.put("files", String.join(" | ", bad));                                      // 7a
        }
        ApiException.throwIfAny(errors);
        lists.requireInList("UC06", u, projectId);                                             // my project, Testing / WaitingForApprove
        Map<String, Object> p = Auth.params(u);
        p.put("ProjectID", projectId);
        p.put("Title", f.title().strip());
        p.put("Detail", f.detail().strip());
        p.put("Priority", f.priority());
        p.put("DueDate", due);
        p.put("RelatedFeature", f.relatedFeature() == null || f.relatedFeature().isBlank() ? null : f.relatedFeature().strip());
        long requirementId = sql.insert("UC06", "8.1", p);                                     // 8.1 'Pending'
        p.put("RequirementID", requirementId);
        for (MultipartFile a : attachments) {
            p.put("AttachmentFile", files.saveNamed(a, "requirements/" + requirementId));
            sql.update("UC06", "8.2", p);                                                      // 8.2 × n
        }
        if (sql.update("UC06", "9.1", p) != 1) {                                               // 9.1 WaitingForApprove
            throw new ApiException(HttpStatus.CONFLICT, "อัปเดตสถานะไม่สำเร็จ");
        }
        sql.update("UC06", "10.1", p);                                                         // 10.1 notify Sale in Charge
        email.send("Sale in Charge of project " + projectId, "มี Requirement ใหม่รออนุมัติ: " + f.title().strip());
        return requirementId;
    }
}
