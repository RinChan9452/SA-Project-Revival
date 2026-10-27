package com.projectsa.uc03;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Service;

import com.projectsa.common.ApiException;
import com.projectsa.common.Auth;
import com.projectsa.common.FileStore;
import com.projectsa.common.SessionUser;
import com.projectsa.common.Sql;

/** UC03 Check Project Details – read only. Query 3.1, 4.1–4.6, 6.1, 6.2 (list: 1.1 in ProjectListApi). */
@Service
public class CheckProjectService {

    public record Download(String fileName, String path) {
    }

    private final Sql sql;

    public CheckProjectService(Sql sql) {
        this.sql = sql;
    }

    private Map<String, Object> access(SessionUser u, long projectId) {
        Map<String, Object> p = Auth.params(u);
        p.put("ProjectID", projectId);
        if (sql.count("UC03", "3.1", p) == 0) {                                        // 3.1 (Alt 3a)
            throw ApiException.notFound("ไม่พบ Project");
        }
        return p;
    }

    /** Step 4: sections 4.1–4.7. Empty sections are empty lists / null; the page shows "ยังไม่มีข้อมูล" (Alt 4a). */
    public Map<String, Object> detail(SessionUser u, long projectId) {
        Map<String, Object> p = access(u, projectId);
        Map<String, Object> out = new LinkedHashMap<>();
        out.put("project", sql.row("UC03", "4.1", p));                                // 4.1–4.4
        out.put("engineers", sql.rows("UC03", "4.2", p));                             // 4.3
        out.put("latestTest", sql.row("UC03", "4.3", p));                             // 4.5
        out.put("requirements", sql.rows("UC03", "4.4", p));                          // 4.6
        out.put("documents", sql.rows("UC03", "4.5", p));                             // 4.7
        List<Map<String, Object>> att = sql.rows("UC03", "4.6", p);                   // 4.7
        att.forEach(a -> a.put("FileName", FileStore.displayName((String) a.get("AttachmentFile"))));
        out.put("attachments", att);
        return out;
    }

    /** Step 6 (6.1): a project document, after the access check. */
    public Download document(SessionUser u, long projectId, long documentId) {
        Map<String, Object> p = access(u, projectId);
        p.put("DocumentID", documentId);
        Map<String, Object> r = sql.row("UC03", "6.1", p);                             // 6.1
        if (r == null) {
            throw ApiException.notFound("ไม่พบเอกสาร");
        }
        return new Download((String) r.get("FileName"), (String) r.get("FilePath"));
    }

    /** Step 6 (6.2): a requirement attachment, after the access check. */
    public Download attachment(SessionUser u, long projectId, long requirementId, String file) {
        Map<String, Object> p = access(u, projectId);
        p.put("RequirementID", requirementId);
        p.put("AttachmentFile", file);
        Map<String, Object> r = sql.row("UC03", "6.2", p);                             // 6.2
        if (r == null) {
            throw ApiException.notFound("ไม่พบไฟล์แนบ");
        }
        String path = (String) r.get("AttachmentFile");
        return new Download(FileStore.displayName(path), path);
    }
}
