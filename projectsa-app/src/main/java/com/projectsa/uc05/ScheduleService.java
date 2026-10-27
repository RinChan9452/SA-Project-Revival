package com.projectsa.uc05;

import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.projectsa.common.ApiException;
import com.projectsa.common.Auth;
import com.projectsa.common.EmailStub;
import com.projectsa.common.ProjectListApi;
import com.projectsa.common.SessionUser;
import com.projectsa.common.Sql;

/** UC05 Set Project Test Calendar Notification – Query 3.1–3.3, 6.1, 6.2, 7.1, 8.1, 9.1 (list: 1.1). */
@Service
public class ScheduleService {

    private final Sql sql;
    private final ProjectListApi lists;
    private final EmailStub email;

    public ScheduleService(Sql sql, ProjectListApi lists, EmailStub email) {
        this.sql = sql;
        this.lists = lists;
        this.email = email;
    }

    /** Step 3: site + customer, team, current appointment. */
    public Map<String, Object> scheduleForm(SessionUser u, long projectId) {
        lists.requireInList("UC05", u, projectId);
        Map<String, Object> p = Auth.params(u);
        p.put("ProjectID", projectId);
        Map<String, Object> out = new LinkedHashMap<>();
        out.put("project", sql.row("UC05", "3.1", p));                       // 3.1
        out.put("team", sql.rows("UC05", "3.2", p));                          // 3.2
        out.put("current", sql.row("UC05", "3.3", p));                        // 3.3
        return out;
    }

    /** Steps 5–9 in one transaction (+ customer email, which the demo only logs). */
    @Transactional
    public long save(SessionUser u, long projectId, ScheduleDetectBlank.ScheduleForm f) {
        Map<String, String> errors = new LinkedHashMap<>();
        ScheduleDetectBlank.Valid v = ScheduleDetectBlank.check(f, LocalDate.now(), errors);   // Step 5 (5a)
        ApiException.throwIfAny(errors);
        lists.requireInList("UC05", u, projectId);                            // assigned + Working / Testing
        Map<String, Object> p = Auth.params(u);
        p.put("ProjectID", projectId);
        sql.update("UC05", "6.1", p);                                         // 6.1 cancel the old appointment
        p.put("TestDate", v.date());
        p.put("StartTime", v.start());
        p.put("EndTime", v.end());
        p.put("Location", f.location().strip());
        p.put("TestDetail", f.testDetail() == null || f.testDetail().isBlank() ? null : f.testDetail().strip());
        p.put("RemindBefore", f.remindBefore());
        long scheduleId = sql.insert("UC05", "6.2", p);                       // 6.2
        if (sql.update("UC05", "7.1", p) != 1) {                              // 7.1 Testing
            throw new ApiException(HttpStatus.CONFLICT, "อัปเดตสถานะไม่สำเร็จ");
        }
        sql.update("UC05", "8.1", p);                                         // 8.1 notify PM + engineers now
        sql.update("UC05", "9.1", p);                                         // 9.1 reminder (SendAt in the future)
        Map<String, Object> project = sql.row("UC05", "3.1", p);              // customer email from 3.1
        email.send((String) project.get("CustomerEmail"), "นัดทดสอบระบบ " + v.date() + " " + v.start() + " ที่ " + f.location().strip());
        return scheduleId;
    }
}
