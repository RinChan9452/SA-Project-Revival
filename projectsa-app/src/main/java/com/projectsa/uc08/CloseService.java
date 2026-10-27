package com.projectsa.uc08;

import java.util.LinkedHashMap;
import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.projectsa.common.ApiException;
import com.projectsa.common.Auth;
import com.projectsa.common.ProjectListApi;
import com.projectsa.common.SessionUser;
import com.projectsa.common.Sql;

/** UC08 Close Project – Query 3.1–3.3, 5.1, 5.2, 6.1, 7.1, 8.1 (list: 1.1). No DetectBlank: Step 5 checks are queries. */
@Service
public class CloseService {

    static final String CANNOT = "ไม่สามารถปิด Project ได้";

    private final Sql sql;
    private final ProjectListApi lists;

    public CloseService(Sql sql, ProjectListApi lists) {
        this.sql = sql;
        this.lists = lists;
    }

    /** Step 3: project + Sale / Presale (3.1), engineers (3.2), latest test (3.3). */
    public Map<String, Object> closeForm(SessionUser u, long projectId) {
        lists.requireInList("UC08", u, projectId);
        Map<String, Object> p = Auth.params(u);
        p.put("ProjectID", projectId);
        Map<String, Object> out = new LinkedHashMap<>();
        out.put("project", sql.row("UC08", "3.1", p));                         // 3.1
        out.put("engineers", sql.rows("UC08", "3.2", p));                      // 3.2
        out.put("latestTest", sql.row("UC08", "3.3", p));                      // 3.3
        return out;
    }

    /** Steps 5–8 in one transaction. */
    @Transactional
    public void close(SessionUser u, long projectId) {
        Map<String, Object> p = Auth.params(u);
        p.put("ProjectID", projectId);
        Object status = sql.value("UC08", "5.1", p);                           // 5.1 Testing + my project
        if (!"Testing".equals(status)) {
            throw new ApiException(HttpStatus.CONFLICT, CANNOT + ": Project ไม่ได้อยู่ในสถานะ Testing");      // Alt 5a
        }
        if (sql.count("UC08", "5.2", p) > 0) {                                 // 5.2 nothing Pending
            throw new ApiException(HttpStatus.CONFLICT, CANNOT + ": ยังมี Requirement รออนุมัติ");           // Alt 5a
        }
        if (sql.update("UC08", "6.1", p) != 1) {                               // 6.1 Finish (guard Status = 'Testing')
            throw new ApiException(HttpStatus.CONFLICT, CANNOT);
        }
        sql.update("UC08", "7.1", p);                                          // 7.1 latest test → Done
        sql.update("UC08", "8.1", p);                                          // 8.1 notify Sale, Presale, engineers
    }
}
