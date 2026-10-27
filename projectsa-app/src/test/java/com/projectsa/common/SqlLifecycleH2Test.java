package com.projectsa.common;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;

/**
 * CLAUDE.md §5 step 2: run all 87 queries of ProjectSA_SQL.sql on H2 (MSSQLServer mode) through the
 * project life cycle. Java port of tools/test_sql.py (same flow, same checks, real clock).
 */
class SqlLifecycleH2Test {

    private Sql sql;
    private JdbcTemplate plain;
    private final Map<String, Object> p = new HashMap<>();
    private final Set<String> ran = new LinkedHashSet<>();
    private final LocalDate today = LocalDate.now();

    @BeforeEach
    void setUp() {
        DriverManagerDataSource ds = new DriverManagerDataSource(
                "jdbc:h2:mem:life" + System.nanoTime() + ";MODE=MSSQLServer;DATABASE_TO_UPPER=FALSE;CASE_INSENSITIVE_IDENTIFIERS=TRUE;DB_CLOSE_DELAY=-1");
        QueryCatalog catalog = QueryCatalog.fromClasspath();
        plain = new JdbcTemplate(ds);
        catalog.schemaStatements().forEach(plain::execute);
        sql = new Sql(new NamedParameterJdbcTemplate(ds), catalog);
        sql.setListener(q -> ran.add(q.key()));
    }

    // ---- helpers: one shared parameter map, like the narrative's @variables
    private Map<String, Object> with(Object... kv) {
        for (int i = 0; i < kv.length; i += 2) {
            p.put((String) kv[i], kv[i + 1]);
        }
        return p;
    }

    private List<Map<String, Object>> rows(String uc, String q, Object... kv) {
        return sql.rows(uc, q, with(kv));
    }

    private Object val(String uc, String q, Object... kv) {
        return sql.value(uc, q, with(kv));
    }

    private int upd(String uc, String q, Object... kv) {
        return sql.update(uc, q, with(kv));
    }

    private long ins(String uc, String q, String key, Object... kv) {
        long id = sql.insert(uc, q, with(kv));
        p.put(key, id);
        return id;
    }

    private String status() {
        return plain.queryForObject("SELECT Status FROM PROJECT WHERE ProjectID = ?", String.class, p.get("ProjectID"));
    }

    private static void tick() {
        try {
            Thread.sleep(15);           // keep CreatedDate values strictly ordered (UC07 Query 11.2 "round")
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    // ---- flows used more than once
    private void uc05(LocalDate date) {
        rows("UC05", "1.1", "LoginID", p.get("T1"));
        rows("UC05", "3.1");
        rows("UC05", "3.2");
        rows("UC05", "3.3");
        upd("UC05", "6.1");
        ins("UC05", "6.2", "ScheduleID", "TestDate", date, "StartTime", LocalTime.of(10, 0), "EndTime", LocalTime.of(12, 0),
                "Location", "Bangkok HQ", "TestDetail", "UAT", "RemindBefore", 1440);
        upd("UC05", "7.1");
        upd("UC05", "8.1");
        upd("UC05", "9.1");
        tick();
    }

    private void uc06(String title, String... files) {
        rows("UC06", "1.1", "LoginID", p.get("PM"));
        rows("UC06", "3.1");
        rows("UC06", "3.2");
        ins("UC06", "8.1", "RequirementID", "Title", title, "Detail", "d", "Priority", "High",
                "DueDate", today.plusDays(90), "RelatedFeature", null);
        for (String f : files) {
            upd("UC06", "8.2", "AttachmentFile", f);
        }
        upd("UC06", "9.1");
        upd("UC06", "10.1");
        tick();
    }

    private void uc07(String title, String decision, String reason) {
        rows("UC07", "1.1", "LoginID", p.get("SALE"));
        Object rid = rows("UC07", "3.1").stream().filter(r -> title.equals(r.get("Title"))).findFirst().orElseThrow()
                .get("RequirementID");
        assertEquals("Pending", val("UC07", "5.1", "RequirementID", rid));
        rows("UC07", "6.1");
        rows("UC07", "6.2");
        assertEquals(1, upd("UC07", "9.1", "Decision", decision, "RejectReason", reason));
        upd("UC07", "10.1");
        if (((Number) val("UC07", "11.1")).intValue() == 0) {
            if (((Number) val("UC07", "11.2")).intValue() > 0) {
                upd("UC07", "11.3");
            } else {
                upd("UC07", "11.4");
            }
        }
        tick();
    }

    @Test
    void wholeLifeCycleRunsEveryQueryOnH2() {
        // ---- F1 Register + F2 Login
        assertEquals(0, rows("F1", "R.1").size());
        plain.execute("INSERT INTO SKILL (SkillName) VALUES ('CCIE'),('CCNA'),('Java'),('Unix')");
        assertEquals(List.of("CCIE", "CCNA", "Java", "Unix"), rows("F1", "R.1").stream().map(r -> r.get("SkillName")).toList());
        Object[][] people = {{"Somchai", "Sale", "SALE"}, {"Pim", "Presale", "PRE"}, {"Kan", "PM", "PM"},
                {"Ton", "TechEngineer", "T1"}, {"Nok", "TechEngineer", "T2"}};
        for (Object[] u : people) {
            String user = ((String) u[0]).toLowerCase();
            assertEquals(0L, ((Number) val("F1", "R.2", "Username", user, "Email", user + "@x.com")).longValue());
            long id = ins("F1", "R.3", "EmployeeID", "FirstName", u[0], "LastName", u[1], "Email", user + "@x.com",
                    "Phone", null, "Username", user, "PasswordHash", "$2a$hash", "Role", u[1]);
            p.put((String) u[2], id);
        }
        upd("F1", "R.4", "EmployeeID", p.get("T1"), "SkillID", 2);
        upd("F1", "R.4", "EmployeeID", p.get("T1"), "SkillID", 4);
        upd("F1", "R.4", "EmployeeID", p.get("T2"), "SkillID", 3);
        // typed skills: R.5 finds an existing one ignoring case; not found → R.6 adds it to SKILL
        assertEquals(3L, ((Number) val("F1", "R.5", "SkillName", "jAVA")).longValue());
        assertEquals(null, val("F1", "R.5", "SkillName", "Docker"));
        long docker = ins("F1", "R.6", "DockerSkillID", "SkillName", "Docker");
        assertEquals(docker, ((Number) val("F1", "R.5", "SkillName", "docker")).longValue());
        upd("F1", "R.4", "EmployeeID", p.get("T2"), "SkillID", docker);
        assertEquals(List.of("CCIE", "CCNA", "Docker", "Java", "Unix"), rows("F1", "R.1").stream().map(r -> r.get("SkillName")).toList());
        assertEquals(1L, ((Number) val("F1", "R.2", "Username", "ton", "Email", "new@x.com")).longValue());
        boolean secondRoleRejected;
        try {
            plain.update("INSERT INTO EMPLOYEE (FirstName, LastName, Email, Username, Password, Role) "
                    + "VALUES ('A','B','a@x.com','dual','h','Sale/PM')");
            secondRoleRejected = false;
        } catch (Exception e) {
            secondRoleRejected = true;
        }
        assertTrue(secondRoleRejected, "one role per account (CHECK constraint)");
        Map<String, Object> login = rows("F2", "L.1", "Username", "kan").get(0);
        assertEquals(p.get("PM"), ((Number) login.get("EmployeeID")).longValue());
        assertEquals("PM", login.get("Role"));
        assertEquals(0, rows("F2", "L.1", "Username", "nobody").size());

        // ---- UC01 .. UC04
        rows("UC01", "2.1", "LoginID", p.get("SALE"));
        rows("UC01", "2.2");
        rows("UC01", "2.3");
        ins("UC01", "5.1", "CustomerID", "CustomerName", "ACME", "CustomerEmail", "it@acme.com");
        ins("UC01", "5.2", "ProjectID", "ProjectName", "Core Network", "ProjectSite", "Bangkok HQ", "PresaleID", p.get("PRE"));
        ran.add("UC01 6.1");                                   // no statement: status saved by 5.2
        upd("UC01", "7.1");
        assertEquals("NewProject", status());
        long p1 = (long) p.get("ProjectID");

        rows("UC02", "1.1", "LoginID", p.get("PRE"));
        rows("UC02", "3.1");
        rows("UC02", "3.2");
        upd("UC02", "8.1", "StartDate", today.plusDays(7), "EndDate", today.plusDays(60), "Objective", "Upgrade",
                "EstimatedBudget", 500000, "SolutionDetail", "Cisco core");
        upd("UC02", "8.2", "DocType", "Solution", "FileName", "s.pdf", "FilePath", "/u/s.pdf", "FileSize", 10);
        upd("UC02", "9.1");
        upd("UC02", "10.1");
        assertEquals("WaitingForAssignEngineer", status());

        for (Object[] r : new Object[][] {{p.get("SALE"), "Sale"}, {p.get("PRE"), "Presale"}, {p.get("PM"), "PM"}}) {
            assertEquals(1, rows("UC03", "1.1", "LoginID", r[0], "Role", r[1], "Keyword", null).size());
        }
        assertEquals(1, rows("UC03", "1.1", "LoginID", p.get("PM"), "Role", "PM", "Keyword", "Core").size());
        assertEquals(1, rows("UC03", "1.1", "Keyword", String.valueOf(p1)).size());
        assertEquals(0, rows("UC03", "1.1", "LoginID", p.get("T1"), "Role", "TechEngineer", "Keyword", null).size());
        assertEquals(1L, ((Number) val("UC03", "3.1", "LoginID", p.get("SALE"), "Role", "Sale")).longValue());
        Map<String, Object> d = rows("UC03", "4.1").get(0);
        assertEquals("Somchai Sale", d.get("SaleInCharge"));
        assertEquals(null, d.get("PMName"));
        for (String q : List.of("4.2", "4.3", "4.4", "4.5", "4.6")) {
            rows("UC03", q);
        }
        assertEquals("s.pdf", rows("UC03", "6.1", "DocumentID", 1).get(0).get("FileName"));

        rows("UC04", "1.1");
        rows("UC04", "3.1");
        assertEquals(2, rows("UC04", "3.2").size());
        rows("UC04", "3.3");
        assertEquals(List.of("Nok"), rows("UC04", "5.1", "SkillID", 3).stream().map(r -> r.get("FirstName")).toList());
        for (String e : List.of("T1", "T2")) {
            upd("UC04", "8.1", "EngineerID", p.get(e));
        }
        upd("UC04", "8.2", "LoginID", p.get("PM"));
        upd("UC04", "9.1");
        upd("UC04", "10.1");
        assertEquals("Working", status());

        // ---- UC05 .. UC07, round 1 and round 2
        LocalDate test1 = today.plusDays(30);
        uc05(test1);
        assertEquals("Testing", status());
        LocalDateTime sendAt = plain.queryForObject(
                "SELECT TOP 1 SendAt FROM NOTIFICATION WHERE Type = 'TestReminder' ORDER BY NotificationID", LocalDateTime.class);
        assertEquals(test1.minusDays(1).atTime(10, 0), sendAt, "UC05 9.1 reminder = test time - RemindBefore");
        assertEquals("นัดทดสอบ " + test1 + " 10:00 ที่ Bangkok HQ", plain.queryForObject(
                "SELECT TOP 1 Message FROM NOTIFICATION WHERE Type = 'TestSchedule' ORDER BY NotificationID", String.class));
        uc06("Add VPN", "vpn.pdf");
        uc06("Add WiFi");
        uc07("Add VPN", "Approved", null);
        assertEquals("WaitingForApprove", status());
        uc07("Add WiFi", "Rejected", "Out of budget");
        assertEquals("NewProject", status());
        List<String> results = plain.queryForList(
                "SELECT Message FROM NOTIFICATION WHERE Type = 'RequirementResult' ORDER BY NotificationID", String.class);
        assertEquals("Requirement \"Add VPN\" : Approved", results.get(0));
        assertEquals("Requirement \"Add WiFi\" : Rejected (เหตุผล: Out of budget)", results.get(1));
        assertEquals("vpn.pdf", rows("UC03", "6.2", "LoginID", p.get("SALE"), "RequirementID", 1, "AttachmentFile", "vpn.pdf")
                .get(0).get("AttachmentFile"));

        upd("UC02", "8.1");
        upd("UC02", "9.1");
        upd("UC04", "8.1", "EngineerID", p.get("T1"));
        upd("UC04", "9.1");
        assertEquals(2, plain.queryForObject("SELECT COUNT(*) FROM PROJECT_ASSIGNMENT", Integer.class));
        tick();
        uc05(today.plusDays(40));
        uc06("Add Monitoring");
        uc07("Add Monitoring", "Rejected", "Not in scope");
        assertEquals("Finish", status(), "round-1 approval must not count in round 2");

        // ---- UC08 on a second project
        ins("UC01", "5.2", "ProjectID", "ProjectName", "Data Center", "ProjectSite", "Chonburi", "PresaleID", p.get("PRE"),
                "CustomerID", 1, "LoginID", p.get("SALE"));
        long p2 = (long) p.get("ProjectID");
        upd("UC02", "8.1");
        upd("UC02", "9.1");
        for (String e : List.of("T1", "T2")) {
            upd("UC04", "8.1", "EngineerID", p.get(e));
        }
        upd("UC04", "8.2", "LoginID", p.get("PM"));
        upd("UC04", "9.1");
        assertEquals(0, rows("UC08", "1.1", "LoginID", p.get("PM")).size());
        LocalDate test3 = today.plusDays(50);
        uc05(test3);
        p.put("ProjectID", p2);
        assertEquals(1, rows("UC08", "1.1", "LoginID", p.get("PM")).size());
        assertEquals(0, rows("UC08", "1.1", "LoginID", p.get("SALE")).size());
        Map<String, Object> c = rows("UC08", "3.1", "LoginID", p.get("PM")).get(0);
        assertEquals("Pim Presale", c.get("PresaleInCharge"));
        assertEquals(2, rows("UC08", "3.2").size());
        assertEquals(java.sql.Date.valueOf(test3), rows("UC08", "3.3").get(0).get("TestDate"));
        assertEquals("Testing", val("UC08", "5.1"));
        assertEquals(0L, ((Number) val("UC08", "5.2")).longValue());
        assertEquals(1, upd("UC08", "6.1"));
        assertEquals(1, upd("UC08", "7.1"));
        upd("UC08", "8.1");
        assertEquals("Finish", status());
        assertEquals(new TreeSet<>(List.of(p.get("SALE"), p.get("PRE"), p.get("T1"), p.get("T2"))),
                new TreeSet<>(plain.queryForList("SELECT RecipientID FROM NOTIFICATION WHERE Type = 'ProjectClosed'", Long.class)));
        assertEquals(0, upd("UC08", "6.1"), "cannot close twice");
        assertEquals(0, rows("UC08", "5.1", "LoginID", p.get("SALE")).size(), "another PM cannot close");
        assertEquals(java.sql.Date.valueOf(test3), rows("UC03", "4.3").get(0).get("TestDate"), "UC03 still shows the Done test");

        // ---- F3 Profile
        p.put("LoginID", p.get("T1"));
        Map<String, Object> me = rows("F3", "P.1").get(0);
        assertEquals("TechEngineer", me.get("Role"));
        assertEquals(List.of("CCNA", "Unix"), rows("F3", "P.2").stream().map(r -> r.get("SkillName")).toList());
        assertEquals(1L, ((Number) val("F3", "P.3", "Email", "kan@x.com")).longValue());
        upd("F3", "P.4", "FirstName", "Tonny", "LastName", "Tech", "Email", "tonny@x.com", "Phone", "0812345678");
        upd("F3", "P.5", "ProfilePicture", "/uploads/profile/4.png");
        upd("F3", "P.6", "PasswordHash", "$2a$new");
        upd("F3", "P.7");
        upd("F1", "R.4", "EmployeeID", p.get("T1"), "SkillID", 1);
        upd("F1", "R.4", "EmployeeID", p.get("T1"), "SkillID", 3);
        me = rows("F3", "P.1").get(0);
        assertEquals("Tonny", me.get("FirstName"));
        assertEquals("TechEngineer", me.get("Role"));
        assertEquals("/uploads/profile/4.png", me.get("ProfilePicture"));
        assertEquals(List.of("CCIE", "Java"), rows("F3", "P.2").stream().map(r -> r.get("SkillName")).toList());

        // ---- F4 Notification Bell
        long unread = ((Number) val("F4", "N.1")).longValue();
        long due = plain.queryForObject("SELECT COUNT(*) FROM NOTIFICATION WHERE RecipientID = ? AND Type <> 'TestReminder'",
                Long.class, p.get("T1"));
        assertEquals(due, unread, "future reminders are hidden");
        List<Map<String, Object>> list = rows("F4", "N.2");
        assertEquals("ProjectClosed", list.get(0).get("Type"));
        upd("F4", "N.3", "NotificationID", list.get(0).get("NotificationID"));
        assertEquals(unread - 1, ((Number) val("F4", "N.1")).longValue());
        assertEquals(0, upd("F4", "N.3", "LoginID", p.get("SALE")), "cannot mark someone else's");
        p.put("LoginID", p.get("T1"));
        upd("F4", "N.4");
        assertEquals(0L, ((Number) val("F4", "N.1")).longValue());

        // ---- coverage: every design query ran
        List<String> missing = new ArrayList<>(sql.catalog().all().keySet());
        missing.removeAll(ran);
        assertEquals(List.of(), missing, "queries never executed");
        assertEquals(87, ran.size());
    }
}
