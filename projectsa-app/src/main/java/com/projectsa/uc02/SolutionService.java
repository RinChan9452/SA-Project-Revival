package com.projectsa.uc02;

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
import com.projectsa.common.FileCheck;
import com.projectsa.common.FileStore;
import com.projectsa.common.ProjectListApi;
import com.projectsa.common.SessionUser;
import com.projectsa.common.Sql;

/** UC02 Store Project Solution Details – Query 1.1, 3.1, 3.2, 8.1, 8.2, 9.1, 10.1. */
@Service
public class SolutionService {

    private final Sql sql;
    private final FileStore files;
    private final ProjectListApi lists;

    public SolutionService(Sql sql, FileStore files, ProjectListApi lists) {
        this.sql = sql;
        this.files = files;
        this.lists = lists;
    }

    /** Step 3: the project and its current solution / documents. */
    public Map<String, Object> solution(SessionUser u, long projectId) {
        Map<String, Object> p = Auth.params(u);
        p.put("ProjectID", projectId);
        Map<String, Object> project = sql.row("UC02", "3.1", p);                         // 3.1
        if (project == null) {
            throw ApiException.notFound("ไม่พบ Project หรือคุณไม่ได้เป็น Presale ของ Project นี้");
        }
        Map<String, Object> out = new LinkedHashMap<>();
        out.put("project", project);
        out.put("documents", sql.rows("UC02", "3.2", p));                                // 3.2
        return out;
    }

    /** Steps 5–10 (one transaction). solutionFiles / requirementFiles = DocType Solution / ProductRequirement. */
    @Transactional
    public void save(SessionUser u, long projectId, SolutionDetectBlank.SolutionForm form,
                     List<MultipartFile> solutionFiles, List<MultipartFile> requirementFiles) {
        lists.requireInList("UC02", u, projectId);                                       // still NewProject + mine
        Map<String, String> errors = new LinkedHashMap<>();
        SolutionDetectBlank.Valid v = SolutionDetectBlank.check(form, errors);           // Step 5 (5a)
        List<String> bad = new ArrayList<>();                                            // Step 7 (7a)
        for (MultipartFile f : solutionFiles) {
            String why = FileCheck.SOLUTION.problem(f);
            if (why != null) bad.add(why);
        }
        for (MultipartFile f : requirementFiles) {
            String why = FileCheck.SOLUTION.problem(f);
            if (why != null) bad.add(why);
        }
        if (!bad.isEmpty()) {
            errors.put("files", String.join(" | ", bad));
        }
        Map<String, Object> p = Auth.params(u);
        p.put("ProjectID", projectId);
        boolean hasSolution = !solutionFiles.isEmpty() || sql.rows("UC02", "3.2", p).stream()
                .anyMatch(d -> "Solution".equals(d.get("DocType")));
        if (!hasSolution) {
            errors.putIfAbsent("files", "ต้องมีเอกสาร Solution อย่างน้อย 1 ไฟล์");             // 7.3
        }
        ApiException.throwIfAny(errors);

        p.put("StartDate", v.start());
        p.put("EndDate", v.end());
        p.put("Objective", form.objective().strip());
        p.put("EstimatedBudget", v.budget());
        p.put("SolutionDetail", form.solutionDetail() == null ? null : form.solutionDetail().strip());
        sql.update("UC02", "8.1", p);                                                     // 8.1
        store(p, projectId, solutionFiles, "Solution");                                   // 8.2 × n
        store(p, projectId, requirementFiles, "ProductRequirement");
        if (sql.update("UC02", "9.1", p) != 1) {                                          // 9.1
            throw new ApiException(HttpStatus.CONFLICT, "อัปเดตสถานะไม่สำเร็จ");
        }
        sql.update("UC02", "10.1", p);                                                    // 10.1 notify every PM
    }

    private void store(Map<String, Object> p, long projectId, List<MultipartFile> list, String docType) {
        for (MultipartFile f : list) {
            p.put("DocType", docType);
            p.put("FileName", f.getOriginalFilename());
            p.put("FilePath", files.save(f, "projects/" + projectId));
            p.put("FileSize", f.getSize());
            sql.update("UC02", "8.2", p);
        }
    }
}
