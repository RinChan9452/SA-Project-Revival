package com.projectsa.uc06;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import org.hamcrest.Matchers;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMultipartHttpServletRequestBuilder;

import com.projectsa.TestData;
import com.projectsa.auth.AuthService;
import com.projectsa.common.Auth;
import com.projectsa.common.SessionUser;
import com.projectsa.uc01.ProjectDetectBlank.ProjectForm;
import com.projectsa.uc01.ProjectService;
import com.projectsa.uc02.SolutionDetectBlank.SolutionForm;
import com.projectsa.uc02.SolutionService;
import com.projectsa.uc04.AssignDetectBlank.AssignForm;
import com.projectsa.uc04.AssignService;
import com.projectsa.uc05.ScheduleDetectBlank.ScheduleForm;
import com.projectsa.uc05.ScheduleService;

/** CLAUDE.md §6.6 UC06 Add Customer New Requirement. */
@SpringBootTest
@AutoConfigureMockMvc
class RequirementApiTest {

    @Autowired MockMvc mvc;
    @Autowired AuthService auth;
    @Autowired ProjectService uc01;
    @Autowired SolutionService uc02;
    @Autowired AssignService uc04;
    @Autowired ScheduleService uc05;
    @Autowired JdbcTemplate jdbc;

    private MockMultipartHttpServletRequestBuilder submit(long id, Map<String, String> fields, MockHttpSession s) {
        MockMultipartHttpServletRequestBuilder b = multipart("/api/projects/" + id + "/requirements");
        fields.forEach(b::param);
        b.session(s);
        return b;
    }

    @Test
    void addRequirements() throws Exception {
        long sale = TestData.register(auth, "u6_sale", Auth.SALE);
        long pre = TestData.register(auth, "u6_pre", Auth.PRESALE);
        long pm = TestData.register(auth, "u6_pm", Auth.PM);
        TestData.register(auth, "u6_pm2", Auth.PM);
        long t1 = TestData.register(auth, "u6_t1", Auth.TECH, "CCIE");
        long id = uc01.create(new SessionUser(sale, Auth.SALE, "a", "b", null),
                new ProjectForm("UC06 Project", null, "UC06 Co", "c@uc06.com", pre, "Site"));
        uc02.save(new SessionUser(pre, Auth.PRESALE, "a", "b", null), id, new SolutionForm("2026-11-01", "2026-12-01", "O", "10", "S"),
                List.of(new MockMultipartFile("f", "s.pdf", "application/pdf", "x".getBytes())), List.of());
        uc04.assign(new SessionUser(pm, Auth.PM, "a", "b", null), id, new AssignForm(List.of(t1)));
        MockHttpSession s = TestData.login(mvc, "u6_pm");
        Map<String, String> ok = Map.of("title", "Add VPN", "detail", "Site-to-site VPN", "priority", "High",
                "dueDate", LocalDate.now().plusDays(30).toString());

        // still Working → not in the UC06 list yet
        mvc.perform(get("/api/projects/" + id + "/requirement-form").session(s)).andExpect(status().isConflict());
        uc05.save(new SessionUser(t1, Auth.TECH, "a", "b", null), id,
                new ScheduleForm(LocalDate.now().plusDays(5).toString(), "10:00", "12:00", "Site", null, 60));

        mvc.perform(get("/api/projects?status=Testing,WaitingForApprove").session(s)).andExpect(jsonPath("$[?(@.ProjectID == " + id + ")]").exists());
        mvc.perform(get("/api/projects/" + id + "/requirement-form").session(TestData.login(mvc, "u6_pm2"))).andExpect(status().isConflict());
        mvc.perform(get("/api/projects/" + id + "/requirement-form").session(TestData.login(mvc, "u6_sale"))).andExpect(status().isForbidden());
        mvc.perform(submit(id, ok, TestData.login(mvc, "u6_t1"))).andExpect(status().isForbidden());
        mvc.perform(get("/api/projects/" + id + "/requirement-form").session(s))
                .andExpect(jsonPath("$.project.PMName").value("u6_pm PM"))
                .andExpect(jsonPath("$.engineers.length()").value(1));

        // Alt 5a, Alt 7a
        mvc.perform(submit(id, Map.of("priority", "Urgent", "dueDate", LocalDate.now().minusDays(1).toString()), s))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fields.title").exists()).andExpect(jsonPath("$.fields.detail").exists())
                .andExpect(jsonPath("$.fields.priority").exists())
                .andExpect(jsonPath("$.fields.dueDate").value("วันที่ต้องการให้เสร็จต้องไม่เป็นวันที่ผ่านมาแล้ว"));
        mvc.perform(submit(id, ok, s).file(new MockMultipartFile("attachments", "virus.exe", "x", "x".getBytes())))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.fields.files").exists());

        // with an attachment, then a second one while already WaitingForApprove (Alt 6a: no file)
        mvc.perform(submit(id, ok, s).file(new MockMultipartFile("attachments", "สเปค VPN.pdf", "application/pdf", "SPEC".getBytes())))
                .andExpect(status().isCreated());
        assertEquals("WaitingForApprove", jdbc.queryForObject("SELECT Status FROM PROJECT WHERE ProjectID=?", String.class, id));
        mvc.perform(submit(id, Map.of("title", "Add WiFi", "detail", "d", "priority", "Low",
                "dueDate", LocalDate.now().plusDays(10).toString()), s)).andExpect(status().isCreated());
        assertEquals(List.of("Pending", "Pending"), jdbc.queryForList(
                "SELECT Status FROM CUSTOMER_REQUIREMENT WHERE ProjectID=? ORDER BY RequirementID", String.class, id));
        assertEquals(pm, jdbc.queryForObject(
                "SELECT TOP 1 CreatedByID FROM CUSTOMER_REQUIREMENT WHERE ProjectID=?", Long.class, id));
        assertEquals(2, jdbc.queryForObject(
                "SELECT COUNT(*) FROM NOTIFICATION WHERE ProjectID=? AND RecipientID=? AND Type='RequirementPending'", Integer.class, id, sale));

        // the attachment is listed and downloadable in UC03 (6.2) under its original name
        long req = jdbc.queryForObject("SELECT TOP 1 RequirementID FROM CUSTOMER_REQUIREMENT WHERE ProjectID=? ORDER BY RequirementID", Long.class, id);
        String path = jdbc.queryForObject("SELECT AttachmentFile FROM REQUIREMENT_ATTACHMENT WHERE RequirementID=?", String.class, req);
        MockHttpSession saleS = TestData.login(mvc, "u6_sale");
        mvc.perform(get("/api/projects/" + id).session(saleS)).andExpect(jsonPath("$.attachments[0].FileName").value("สเปค VPN.pdf"));
        mvc.perform(get("/api/projects/" + id + "/requirements/" + req + "/attachment").param("file", path).session(saleS))
                .andExpect(status().isOk()).andExpect(content().bytes("SPEC".getBytes()))
                .andExpect(header().string("Content-Disposition", Matchers.containsString("UTF-8")));
    }
}
