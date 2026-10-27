package com.projectsa;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultMatcher;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.test.web.servlet.request.MockMultipartHttpServletRequestBuilder;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * CLAUDE.md §6.8 – the whole life cycle through the HTTP API only (what the pages do), on a fresh database:
 * F1 register → F2 login → UC01 → UC02 → UC04 → UC05 → round 1 (UC06 ×2, UC07 approve + reject → NewProject)
 * → round 2 (UC02, UC04, UC05, UC06, UC07 reject → Finish) → UC08 on a second project → role checks → F4 bell.
 */
@SpringBootTest(properties = "spring.datasource.url=jdbc:h2:mem:acceptance;MODE=MSSQLServer;DATABASE_TO_UPPER=FALSE;"
        + "CASE_INSENSITIVE_IDENTIFIERS=TRUE;DB_CLOSE_DELAY=-1")
@AutoConfigureMockMvc
class LifecycleAcceptanceTest {

    @Autowired MockMvc mvc;
    @Autowired ObjectMapper json;
    @Autowired JdbcTemplate jdbc;

    private JsonNode call(MockHttpServletRequestBuilder req, MockHttpSession s, ResultMatcher expect) throws Exception {
        if (s != null) {
            req.session(s);
        }
        var res = mvc.perform(req).andExpect(expect).andReturn().getResponse();
        String type = res.getContentType();
        return type == null || !type.contains("json") ? null : json.readTree(res.getContentAsString());
    }

    private JsonNode get200(String url, MockHttpSession s) throws Exception {
        return call(get(url), s, status().isOk());
    }

    private JsonNode send(MockHttpServletRequestBuilder req, MockHttpSession s, Object body, ResultMatcher expect) throws Exception {
        return call(req.contentType(MediaType.APPLICATION_JSON).content(json.writeValueAsString(body)), s, expect);
    }

    private MockHttpSession register(String user, String role, List<String> skills) throws Exception {
        send(post("/api/register"), null, Map.of("firstName", user, "lastName", role, "email", user + "@acc.local", "phone", "",
                "username", user, "password", "secret1", "role", role, "skills", skills), status().isCreated());
        MockHttpSession s = (MockHttpSession) mvc.perform(post("/api/login").contentType(MediaType.APPLICATION_JSON)
                .content(json.writeValueAsString(Map.of("username", user, "password", "secret1"))))
                .andExpect(status().isOk()).andReturn().getRequest().getSession();
        return s;
    }

    private long id(MockHttpSession s) throws Exception {
        return get200("/api/me", s).get("EmployeeID").asLong();
    }

    private String projectStatus(long project) {
        return jdbc.queryForObject("SELECT Status FROM PROJECT WHERE ProjectID = ?", String.class, project);
    }

    private List<Long> recipients(long project, String type) {
        return jdbc.queryForList("SELECT RecipientID FROM NOTIFICATION WHERE ProjectID = ? AND Type = ? ORDER BY RecipientID",
                Long.class, project, type);
    }

    private static void tick() throws InterruptedException {
        Thread.sleep(20);               // CreatedDate ordering for the UC07 round (Query 11.2)
    }

    private long uc01(MockHttpSession sale, String name, long presale, JsonNode customer) throws Exception {
        Map<String, Object> body = customer == null
                ? Map.of("projectName", name, "newCustomerName", "ACME", "newCustomerEmail", "it@acme.com",
                        "presaleId", presale, "projectSite", "Bangkok HQ")
                : Map.of("projectName", name, "customerId", customer.asLong(), "presaleId", presale, "projectSite", "Chonburi DC");
        return send(post("/api/projects"), sale, body, status().isCreated()).get("ProjectID").asLong();
    }

    private void uc02(MockHttpSession pre, long project, boolean withFile) throws Exception {
        MockMultipartHttpServletRequestBuilder req = multipart("/api/projects/" + project + "/solution");
        req.param("startDate", LocalDate.now().plusDays(7).toString()).param("endDate", LocalDate.now().plusDays(90).toString())
                .param("objective", "Upgrade core network").param("estimatedBudget", "500000").param("solutionDetail", "Cisco 9500 core");
        if (withFile) {
            req.file(new MockMultipartFile("solutionFiles", "solution.pdf", "application/pdf", "PDF".getBytes()));
        }
        call(req, pre, status().isOk());
    }

    private void uc04(MockHttpSession pm, long project, long... engineers) throws Exception {
        get200("/api/projects/" + project + "/assign-form", pm);
        send(post("/api/projects/" + project + "/assignments"), pm, Map.of("engineerIds", engineers), status().isOk());
    }

    private void uc05(MockHttpSession tech, long project, LocalDate date) throws Exception {
        get200("/api/projects/" + project + "/schedule-form", tech);
        send(post("/api/projects/" + project + "/schedules"), tech, Map.of("testDate", date.toString(), "startTime", "10:00",
                "endTime", "12:00", "location", "Bangkok HQ", "testDetail", "UAT", "remindBefore", 1440), status().isCreated());
        tick();
    }

    private long uc06(MockHttpSession pm, long project, String title) throws Exception {
        MockMultipartHttpServletRequestBuilder req = multipart("/api/projects/" + project + "/requirements");
        req.param("title", title).param("detail", "detail").param("priority", "High")
                .param("dueDate", LocalDate.now().plusDays(60).toString());
        long r = call(req, pm, status().isCreated()).get("RequirementID").asLong();
        tick();
        return r;
    }

    private String uc07(MockHttpSession sale, long project, long requirement, String decision, String reason) throws Exception {
        get200("/api/requirements/" + requirement + "?project=" + project, sale);
        Map<String, Object> body = reason == null ? Map.of("projectId", project, "decision", decision)
                : Map.of("projectId", project, "decision", decision, "rejectReason", reason);
        String st = send(put("/api/requirements/" + requirement + "/decision"), sale, body, status().isOk()).get("ProjectStatus").asText();
        tick();
        return st;
    }

    @Test
    void wholeLifeCycleThroughTheApi() throws Exception {
        // ---- F1 + F2
        MockHttpSession sale = register("acc_sale", "Sale", List.of());
        MockHttpSession pre = register("acc_pre", "Presale", List.of());
        MockHttpSession pm = register("acc_pm", "PM", List.of());
        MockHttpSession t1 = register("acc_t1", "TechEngineer", List.of("CCIE", "CCNA"));
        MockHttpSession t2 = register("acc_t2", "TechEngineer", List.of("Java"));
        MockHttpSession outsider = register("acc_t3", "TechEngineer", List.of("Linux"));
        long saleId = id(sale), preId = id(pre), pmId = id(pm), t1Id = id(t1), t2Id = id(t2);

        // ---- UC01 → UC02 → UC04 → UC05
        long p1 = uc01(sale, "Core Network", preId, null);
        assertEquals("NewProject", projectStatus(p1));
        assertEquals(List.of(preId), recipients(p1, "NewProject"));
        uc02(pre, p1, true);
        assertEquals("WaitingForAssignEngineer", projectStatus(p1));
        assertEquals(List.of(pmId), recipients(p1, "WaitingAssign"));
        uc04(pm, p1, t1Id, t2Id);
        assertEquals("Working", projectStatus(p1));
        assertEquals(List.of(t1Id, t2Id), recipients(p1, "Assigned"));
        LocalDate test1 = LocalDate.now().plusDays(30);
        uc05(t1, p1, test1);
        assertEquals("Testing", projectStatus(p1));
        assertEquals(List.of(pmId, t1Id, t2Id), recipients(p1, "TestSchedule"));
        assertEquals(test1.minusDays(1).atTime(10, 0), jdbc.queryForObject(
                "SELECT TOP 1 SendAt FROM NOTIFICATION WHERE ProjectID = ? AND Type = 'TestReminder'", LocalDateTime.class, p1));

        // ---- role checks: unassigned engineer cannot see / download; wrong status refused; wrong role 403
        call(get("/api/projects/" + p1), outsider, status().isNotFound());
        long doc = jdbc.queryForObject("SELECT TOP 1 DocumentID FROM PROJECT_DOCUMENT WHERE ProjectID = ?", Long.class, p1);
        call(get("/api/projects/" + p1 + "/documents/" + doc), outsider, status().isNotFound());
        call(get("/api/projects/" + p1 + "/documents/" + doc), t1, status().isOk());
        send(post("/api/projects/" + p1 + "/assignments"), pm, Map.of("engineerIds", List.of(t1Id)), status().isConflict());
        call(get("/api/projects/create-form"), pm, status().isForbidden());

        // ---- round 1: two requirements (second one while already WaitingForApprove)
        long vpn = uc06(pm, p1, "Add VPN");
        assertEquals("WaitingForApprove", projectStatus(p1));
        long wifi = uc06(pm, p1, "Add WiFi");
        assertEquals(List.of(saleId, saleId), recipients(p1, "RequirementPending"));
        assertEquals("WaitingForApprove", uc07(sale, p1, vpn, "Approved", null));
        assertEquals("NewProject", uc07(sale, p1, wifi, "Rejected", "Out of budget"));
        List<String> results = jdbc.queryForList("SELECT Message FROM NOTIFICATION WHERE ProjectID = ? AND Type = 'RequirementResult' "
                + "AND RecipientID = ? ORDER BY NotificationID", String.class, p1, pmId);
        assertEquals(List.of("Requirement \"Add VPN\" : Approved", "Requirement \"Add WiFi\" : Rejected (เหตุผล: Out of budget)"), results);
        assertTrue(recipients(p1, "RequirementResult").contains(preId), "Presale told to revise the solution");

        // ---- round 2: redesign, reassign (no duplicates), re-test (old one Cancelled), reject → Finish
        uc02(pre, p1, false);
        uc04(pm, p1, t1Id);
        assertEquals(2, jdbc.queryForObject("SELECT COUNT(*) FROM PROJECT_ASSIGNMENT WHERE ProjectID = ?", Integer.class, p1));
        uc05(t2, p1, LocalDate.now().plusDays(45));
        assertEquals(List.of("Cancelled", "Scheduled"),
                jdbc.queryForList("SELECT Status FROM TEST_SCHEDULE WHERE ProjectID = ? ORDER BY ScheduleID", String.class, p1));
        long mon = uc06(pm, p1, "Add Monitoring");
        assertEquals("Finish", uc07(sale, p1, mon, "Rejected", "Not in scope"), "round-1 approval must not count");

        // ---- round 3: UC08 on a second project (existing customer)
        JsonNode customer = get200("/api/projects/create-form", sale).get("customers").get(0).get("CustomerID");
        long p2 = uc01(sale, "Data Center", preId, customer);
        uc02(pre, p2, true);
        uc04(pm, p2, t1Id, t2Id);
        call(post("/api/projects/" + p2 + "/close"), pm, status().isConflict());                     // not Testing yet
        LocalDate test3 = LocalDate.now().plusDays(10);
        uc05(t1, p2, test3);
        get200("/api/projects/" + p2 + "/close-form", pm);
        call(post("/api/projects/" + p2 + "/close"), pm, status().isOk());
        assertEquals("Finish", projectStatus(p2));
        assertEquals("Done", jdbc.queryForObject("SELECT Status FROM TEST_SCHEDULE WHERE ProjectID = ?", String.class, p2));
        assertEquals(List.of(saleId, preId, t1Id, t2Id), recipients(p2, "ProjectClosed"));
        call(post("/api/projects/" + p2 + "/close"), pm, status().isConflict());                     // twice
        assertEquals(test3.toString(), get200("/api/projects/" + p2, sale).get("latestTest").get("TestDate").asText(),
                "UC03 still shows the Done test");

        // ---- F3 + F4: profile keeps the role; the bell hides future reminders and marks read
        send(put("/api/profile"), t1, Map.of("firstName", "Ton", "lastName", "Tech", "email", "ton@acc.local"), status().isOk());
        assertEquals("TechEngineer", get200("/api/me", t1).get("Role").asText());
        long unread = get200("/api/notifications/unread-count", t1).get("Unread").asLong();
        long dueForT1 = jdbc.queryForObject("SELECT COUNT(*) FROM NOTIFICATION WHERE RecipientID = ? AND Type <> 'TestReminder'", Long.class, t1Id);
        assertEquals(dueForT1, unread, "future reminders are not on the bell yet");
        JsonNode first = get200("/api/notifications", t1).get(0);
        assertEquals("ProjectClosed", first.get("Type").asText());
        call(put("/api/notifications/" + first.get("NotificationID").asLong() + "/read"), t1, status().isOk());
        assertEquals(unread - 1, get200("/api/notifications/unread-count", t1).get("Unread").asLong());
        call(put("/api/notifications/read-all"), t1, status().isOk());
        assertEquals(0, get200("/api/notifications/unread-count", t1).get("Unread").asLong());
    }
}
